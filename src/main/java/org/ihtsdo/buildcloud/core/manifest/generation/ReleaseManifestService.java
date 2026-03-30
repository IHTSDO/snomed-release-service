package org.ihtsdo.buildcloud.core.manifest.generation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.ihtsdo.buildcloud.core.dao.ReleaseCenterDAO;
import org.ihtsdo.buildcloud.core.entity.ManifestConfig;
import org.ihtsdo.buildcloud.core.entity.ReleaseCenter;
import org.ihtsdo.buildcloud.core.manifest.generation.domain.ReleaseContext;
import org.ihtsdo.buildcloud.core.manifest.generation.domain.ReleaseManifest;
import org.ihtsdo.buildcloud.core.manifest.generation.domain.ReleaseManifestFile;
import org.ihtsdo.buildcloud.core.manifest.generation.domain.ReleaseManifestFolder;
import org.ihtsdo.buildcloud.core.service.TermServerService;
import org.ihtsdo.buildcloud.core.service.build.RF2Constants;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.terminologyserver.Page;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.CodeSystem;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.ConceptMiniPojo;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.RefsetMember;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static java.lang.String.format;
import static org.ihtsdo.buildcloud.core.manifest.generation.CollectionUtils.orEmpty;


/**
 * Service for generating manifests to be used in the snomed-release-service.
 * Manifests are used to specify the layout of an RF2 release.
 */
@Service
public class ReleaseManifestService {

    private static final String MEMBER_ANNOTATION_STRING_REFSET_ID = "1292995002";

    private static final String[][] SNAPSHOT_TO_FULL_REPLACEMENTS = {
            {"_Snapshot", "_Full"},
            {"Snapshot_", "Full_"},
            {"Snapshot-", "Full-"},
    };

    private static final String[][] DELTA_TO_SNAPSHOT_REPLACEMENTS = {
            {"_Delta", "_Snapshot"},
            {"Delta_", "Snapshot_"},
            {"Delta-", "Snapshot-"},
    };

    private static final String[][] DELTA_TO_FULL_REPLACEMENTS = {
            {"_Delta", "_Full"},
            {"Delta_", "Full_"},
            {"Delta-", "Full-"},
    };

    private static final ObjectMapper JSON_TREE_MAPPER = new ObjectMapper();

    private final MappingJackson2XmlHttpMessageConverter xmlConverter;

    private final TermServerService termServerService;

    private final ReleaseCenterDAO releaseCenterDAO;

    @Autowired
    public ReleaseManifestService(MappingJackson2XmlHttpMessageConverter xmlConverter, TermServerService termServerService, ReleaseCenterDAO releaseCenterDAO) {
        this.xmlConverter = xmlConverter;
        this.termServerService = termServerService;
        this.releaseCenterDAO = releaseCenterDAO;
    }

    @Transactional(readOnly = true)
    public String generateManifestXml(ManifestConfig manifestConfig, String releaseCenterKey, String branchPath, String effectiveTime, boolean isDailyBuild, boolean betaRelease
    ) throws BusinessServiceException, RestClientException {

        ManifestFilenameContext filenameContext = new ManifestFilenameContext(effectiveTime, manifestConfig.getProductNamespace(), betaRelease, isDailyBuild, manifestConfig.isDerivativeProduct());
        InitialManifest initial = createInitialManifest(manifestConfig, filenameContext, isDailyBuild, betaRelease);
        CodeSystem codeSystem = resolveCodeSystem(releaseCenterKey);

        addCoreComponents(codeSystem, initial.terminologyFolder(), filenameContext);

        Map<String, ConceptMiniPojo> refsets = new HashMap<>(termServerService.getRefsetsWithTypeInformation(branchPath, null));
        if (!CollectionUtils.isEmpty(manifestConfig.getExcludedRefsetsAsList())) {
            manifestConfig.getExcludedRefsetsAsList().forEach(refsets::remove);
        }

        Set<String> refsetsWithMissingExportConfiguration = new HashSet<>();
        ReleaseContext releaseContext = new ReleaseContext(codeSystem, effectiveTime, termServerService);
        ReleaseManifestFolder refsetFolder = addRefsets(releaseContext, initial.contentFolder(), refsets, filenameContext, refsetsWithMissingExportConfiguration);

        addEmptyMemberAnnotationStringRefsetIfMissing(refsets, codeSystem, initial.contentFolder(), refsetFolder, filenameContext);

        if (!refsetsWithMissingExportConfiguration.isEmpty()) {
            throw new BusinessServiceException(format("Unable to generate build manifest file because the following refsets do not have an export configuration: %s",
                    refsetsWithMissingExportConfiguration));
        }

        if (isDailyBuild) {
            cloneFolderTreeWithRenames(initial.contentFolder(), initial.rootFolder().getOrAddFolder(RF2Constants.SNAPSHOT), DELTA_TO_SNAPSHOT_REPLACEMENTS);
            cloneFolderTreeWithRenames(initial.contentFolder(), initial.rootFolder().getOrAddFolder(RF2Constants.FULL), DELTA_TO_FULL_REPLACEMENTS);
        } else {
            cloneFolderTreeWithRenames(initial.contentFolder(), initial.rootFolder().getOrAddFolder(RF2Constants.FULL), SNAPSHOT_TO_FULL_REPLACEMENTS);
        }

        return writeManifestXml(initial.manifest(), codeSystem);
    }

    private InitialManifest createInitialManifest(ManifestConfig manifestConfig, ManifestFilenameContext filenameContext, boolean isDailyBuild, boolean betaRelease) {
        String effectiveTime = filenameContext.effectiveTime();
        String formattedProductName = manifestConfig.getProductName().replace(" ", "");
        String preOrProductionType = betaRelease ? "PREPRODUCTION" : "PRODUCTION";
        String namespaceSegment = manifestConfig.isIncludeProductNamespaceInPackage() ? "_" + manifestConfig.getProductNamespace() : "";
        String rootFolderName = String.format("%sSnomedCT_%s_%s%s_%sT120000Z",
                betaRelease ? "x" : "",
                formattedProductName,
                isDailyBuild ? "DAILYBUILD_BETA" : preOrProductionType,
                namespaceSegment,
                manifestConfig.getPackageEffectiveTimeSnomedFormat() != null ? manifestConfig.getPackageEffectiveTimeSnomedFormat() : effectiveTime);
        ReleaseManifestFolder rootFolder = new ReleaseManifestFolder(rootFolderName);
        ReleaseManifest manifest = new ReleaseManifest(rootFolder);
        rootFolder.getOrAddFile(format("Readme_en_%s.txt", effectiveTime)).clearSource();
        if (!isDailyBuild && !betaRelease) {
            rootFolder.getOrAddFile("release_package_information.json").clearSource();
        }

        ReleaseManifestFolder contentFolder = rootFolder.getOrAddFolder(isDailyBuild ? RF2Constants.DELTA : RF2Constants.SNAPSHOT);
        ReleaseManifestFolder terminologyFolder = contentFolder.getOrAddFolder("Terminology");
        terminologyFolder.getOrAddFile(getCoreComponentFilename(filenameContext, "Concept", ""));

        return new InitialManifest(manifest, rootFolder, contentFolder, terminologyFolder);
    }

    private CodeSystem resolveCodeSystem(String releaseCenterKey) throws BusinessServiceException {
        ReleaseCenter releaseCenter = releaseCenterDAO.find(releaseCenterKey);
        List<CodeSystem> codeSystems = termServerService.getCodeSystems();
        return codeSystems.stream()
                .filter(c -> c.getShortName().equals(releaseCenter.getCodeSystem()))
                .findFirst()
                .orElseThrow(() -> new BusinessServiceException("No code system found for branch release center " + releaseCenterKey));
    }

    private void addEmptyMemberAnnotationStringRefsetIfMissing(Map<String, ConceptMiniPojo> refsets, CodeSystem codeSystem, ReleaseManifestFolder contentFolder,
                                                               ReleaseManifestFolder refsetFolder, ManifestFilenameContext filenameContext) throws RestClientException {
        if (refsets.containsKey(MEMBER_ANNOTATION_STRING_REFSET_ID)) {
            return;
        }
        ConceptMiniPojo annotationRefset = termServerService.getConcepts(MEMBER_ANNOTATION_STRING_REFSET_ID, codeSystem.getBranchPath(), null).getItems().iterator().next();
        ReleaseManifestFolder outputFolder = getRefsetOutputFolder("Metadata", contentFolder, refsetFolder);
        ReleaseManifestFile refsetFile = getRefsetFile(filenameContext, "MemberAnnotationStringValue", "", "sscs", outputFolder);
        addRefsetAndFields(annotationRefset, refsetFile, List.of("referencedMemberId", "languageDialectCode", "typeId", "value"));
    }

    private String writeManifestXml(ReleaseManifest manifest, CodeSystem codeSystem) throws BusinessServiceException {
        ObjectMapper objectMapper = xmlConverter.getObjectMapper()
                .configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        try {
            return objectMapper.writeValueAsString(manifest).replace(" xmlns=\"\"", "");
        } catch (JsonProcessingException e) {
            throw new BusinessServiceException(format("Failed to write release manifest as XML for code system %s.", codeSystem.getShortName()), e);
        }
    }

    private void addCoreComponents(CodeSystem codeSystem, ReleaseManifestFolder terminologyFolder, ManifestFilenameContext filenameContext) {
        List<String> languageCodes = new ArrayList<>(codeSystem.getLanguages().keySet());
        for (String languageCode : languageCodes) {
            terminologyFolder.getOrAddFile(getCoreComponentFilename(filenameContext, "Description", format("-%s", languageCode)))
                    .addLanguageCode(languageCode);
        }
        for (String languageCode : languageCodes) {
            terminologyFolder.getOrAddFile(getCoreComponentFilename(filenameContext, "TextDefinition", format("-%s", languageCode)))
                    .addLanguageCode(languageCode);
        }
        terminologyFolder.getOrAddFile(getCoreComponentFilename(filenameContext, "Identifier", ""));
        terminologyFolder.getOrAddFile(getCoreComponentFilename(filenameContext, "StatedRelationship", ""));
        terminologyFolder.getOrAddFile(getCoreComponentFilename(filenameContext, "Relationship", ""));
        terminologyFolder.getOrAddFile(getCoreComponentFilename(filenameContext, "RelationshipConcreteValues", ""));
        // OWLExpression file is added by the refset logic
    }

    private ReleaseManifestFolder addRefsets(ReleaseContext releaseContext, ReleaseManifestFolder snapshotFolder, Map<String, ConceptMiniPojo> refsets,
                                             ManifestFilenameContext filenameContext, Set<String> refsetsWithMissingExportConfiguration) {

        ReleaseManifestFolder refsetFolder = snapshotFolder.getOrAddFolder("Refset");
        for (ConceptMiniPojo refset : refsets.values()) {
            addRefset(releaseContext, snapshotFolder, filenameContext, refsetsWithMissingExportConfiguration, refset, refsetFolder);
        }
        return refsetFolder;
    }

    @SuppressWarnings("unchecked")
    private void addRefset(ReleaseContext releaseContext, ReleaseManifestFolder snapshotFolder, ManifestFilenameContext filenameContext,
                           Set<String> refsetsWithMissingExportConfiguration, ConceptMiniPojo refset, ReleaseManifestFolder refsetFolder) {

        TermServerService snowstormClient = releaseContext.snowstormClient();
        CodeSystem codeSystem = releaseContext.codeSystem();

        String exportDir = null;
        String exportName = null;
        String languageCode = null;
        String fieldTypes = null;
        List<String> fieldNameList = null;

        Map<String, Object> fileConfiguration = getRefsetFileConfiguration(refset);
        if (!fileConfiguration.isEmpty()) {
            exportDir = (String) fileConfiguration.get("exportDir");
            exportName = (String) fileConfiguration.get("name");
            fieldTypes = (String) fileConfiguration.get("fieldTypes");
            fieldNameList = (List<String>) fileConfiguration.get("fieldNameList");

            if ("Language".equals(exportName)) {
                languageCode = getLangRefsetLanguageCode(codeSystem, snowstormClient, refset);
            } else {
                languageCode = "";
            }

            // Workaround for International maps being outdated
            if ("SimpleMapFromSCT".equals(exportName) || "SimpleMapToSCT".equals(exportName)) {
                exportName = "SimpleMap";
                fieldNameList = List.of("mapTarget");
            }
        }
        if (exportDir == null || fieldTypes == null) {
            refsetsWithMissingExportConfiguration.add(refset.getConceptId());
            return;
        }
        ReleaseManifestFolder outputFolder = getRefsetOutputFolder(exportDir, snapshotFolder, refsetFolder);
        ReleaseManifestFile refsetFile = getRefsetFile(filenameContext, exportName, languageCode, fieldTypes, outputFolder);
        addRefsetAndFields(refset, refsetFile, fieldNameList);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getRefsetFileConfiguration(ConceptMiniPojo refset) {
        Map<String, Object> extraFields = refset.getExtraFields();
        if (extraFields != null) {
            Map<String, Object> referenceSetType = (Map<String, Object>) extraFields.get("referenceSetType");
            if (referenceSetType != null && referenceSetType.get("fileConfiguration") != null) {
                return (Map<String, Object>) referenceSetType.get("fileConfiguration");
            }
        }
        return Collections.emptyMap();
    }

    private String getLangRefsetLanguageCode(CodeSystem codeSystem, TermServerService snowstormClient, ConceptMiniPojo refset) {
        String languageCode = "-en";
        Page<RefsetMember> refsetMembers = snowstormClient.getRefsetMembers(refset.getConceptId(), codeSystem.getBranchPath(), true, 5, null);
        if (refsetMembers.getTotal() > 0) {
            RefsetMember firstLanguageRefsetMember = refsetMembers.getItems().get(0);
            Object referencedComponent = firstLanguageRefsetMember.getReferencedComponent();
            if (referencedComponent != null) {
                JsonNode jsonNode = JSON_TREE_MAPPER.valueToTree(referencedComponent);
                languageCode = String.format("-%s", jsonNode.get("lang").asText());
            }
        }
        return languageCode;
    }

    private void addRefsetAndFields(ConceptMiniPojo refset, ReleaseManifestFile refsetFile, List<String> fieldNameList) {
        if (refsetFile.getField() == null) {
            for (String fieldName : fieldNameList) {
                refsetFile.addField(fieldName);
            }
        }
        refsetFile.addRefset(refset.getConceptId(), refset.getPt().getTerm());
    }

    private ReleaseManifestFile getRefsetFile(ManifestFilenameContext filenameContext, String exportName, String languageCode, String fieldTypes, ReleaseManifestFolder outputFolder) {
        String refsetFileName = getRefsetFilename(filenameContext, exportName, languageCode, fieldTypes);
        return outputFolder.getOrAddFile(refsetFileName);
    }

    private ReleaseManifestFolder getRefsetOutputFolder(String exportDir, ReleaseManifestFolder snapshotFolder, ReleaseManifestFolder refsetFolder) {
        ReleaseManifestFolder outputFolder = exportDir.startsWith("/") ? snapshotFolder : refsetFolder;
        String[] split = exportDir.split("/");
        for (String folderName : split) {
            if (!folderName.isEmpty()) {
                outputFolder = outputFolder.getOrAddFolder(folderName);
            }
        }
        return outputFolder;
    }

    private void cloneFolderTreeWithRenames(ReleaseManifestFolder source, ReleaseManifestFolder target, String[][] replacements) {
        for (ReleaseManifestFile file : orEmpty(source.getFile())) {
            target.addFile(file.copy(applyReplacements(file.getName(), replacements)));
        }
        for (ReleaseManifestFolder folder : orEmpty(source.getFolder())) {
            cloneFolderTreeWithRenames(folder, target.getOrAddFolder(folder.getName()), replacements);
        }
    }

    private static String applyReplacements(String name, String[][] pairs) {
        String result = name;
        for (String[] pair : pairs) {
            result = result.replace(pair[0], pair[1]);
        }
        return result;
    }

    private String getCoreComponentFilename(ManifestFilenameContext ctx, String exportName, String langPostfix) {
        if (ctx.isDerivative()) {
            return format("%ssct2_%s_%s%s%s_%s_%s.txt", ctx.betaRelease() ? "x" : "", exportName, ctx.productNamespace(), ctx.isDailyBuild() ? RF2Constants.DELTA : RF2Constants.SNAPSHOT, langPostfix, RF2Constants.INT, ctx.effectiveTime());
        }
        return format("%ssct2_%s_%s%s_%s_%s.txt", ctx.betaRelease() ? "x" : "", exportName, ctx.isDailyBuild() ? RF2Constants.DELTA : RF2Constants.SNAPSHOT, langPostfix, ctx.productNamespace(), ctx.effectiveTime());
    }

    private String getRefsetFilename(ManifestFilenameContext ctx, String exportName, String languageCode, String fieldTypes) {
        String prefix = "der2";
        if ("OWLExpression".equals(exportName)) {
            prefix = "sct2";
        }
        if (ctx.isDerivative()) {
            return format("%s%s_%sRefset_%s%s%s%s_%s_%s.txt", ctx.betaRelease() ? "x" : "", prefix, fieldTypes, ctx.productNamespace(), exportName, ctx.isDailyBuild() ? RF2Constants.DELTA : RF2Constants.SNAPSHOT, languageCode, RF2Constants.INT, ctx.effectiveTime());
        }
        return format("%s%s_%sRefset_%s%s%s_%s_%s.txt", ctx.betaRelease() ? "x" : "", prefix, fieldTypes, exportName, ctx.isDailyBuild() ? RF2Constants.DELTA : RF2Constants.SNAPSHOT, languageCode, ctx.productNamespace(), ctx.effectiveTime());
    }

    private record ManifestFilenameContext(String effectiveTime, String productNamespace, boolean betaRelease, boolean isDailyBuild, boolean isDerivative) {
    }

    private record InitialManifest(ReleaseManifest manifest, ReleaseManifestFolder rootFolder, ReleaseManifestFolder contentFolder, ReleaseManifestFolder terminologyFolder) {
    }
}
