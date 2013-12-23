CREATE DATABASE  IF NOT EXISTS `id_gen` /*!40100 DEFAULT CHARACTER SET utf8 */;
USE `id_gen`;

DROP TABLE IF EXISTS `CONIDMAP`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CONIDMAP` (
  `CONCEPTID` varchar(25) DEFAULT NULL,
  `CTV3ID` varchar(5) DEFAULT NULL,
  `SNOMEDID` varchar(8) DEFAULT NULL,
  `CODE` varchar(36) DEFAULT NULL,
  `GID` varchar(25) DEFAULT NULL,
  `EXECUTION_ID` char(32) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qa_run`
--

DROP TABLE IF EXISTS `ID_BASE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ID_BASE` (
  `COUNTER_TYPE` varchar(10) DEFAULT NULL,
  `VAL` varchar(8) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qa_policy_log`
--

DROP TABLE IF EXISTS `SCTID_BASE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SCTID_BASE` (
  `PARTITIONNR` int(11) DEFAULT NULL,
  `VAL` int(11) DEFAULT NULL,
  `NAMESPACE` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `qa_comment`
--

DROP TABLE IF EXISTS `SCTID_IDENTIFIER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SCTID_IDENTIFIER` (
  `PARTITION_ID` char(2) DEFAULT NULL,
  `NAMESPACE_ID` char(7) DEFAULT NULL,
  `ARTIFACT_ID` varchar(18) DEFAULT NULL,
  `RELEASE_ID` varchar(8) DEFAULT NULL,
  `ITEM_ID` int(11) DEFAULT NULL,
  `SCTID` varchar(18) DEFAULT NULL,
  `CODE` varchar(512) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
