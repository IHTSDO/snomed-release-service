package org.ihtsdo.buildcloud.service;

import org.apache.commons.codec.DecoderException;
import org.ihtsdo.buildcloud.service.termserver.GatherInputRequestPojo;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public interface ReleaseService {

    void createReleasePackage(String releaseCenter, String productKey, GatherInputRequestPojo gatherInputRequestPojo, SimpMessagingTemplate messagingTemplate) throws BusinessServiceException, IOException, NoSuchAlgorithmException, JAXBException, DecoderException;

}
