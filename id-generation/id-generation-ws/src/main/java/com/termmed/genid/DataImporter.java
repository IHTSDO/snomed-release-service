package com.termmed.genid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.axis2.wsdl.codegen.writer.CSvcSkeletonWriter;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;

import com.termmed.genid.data.ConidMap;
import com.termmed.genid.data.SctIdBase;
import com.termmed.genid.util.MyBatisUtil;

public class DataImporter {
	
	
	public static void main(String[] args) {
		importCsvToConidmap();
	}

	private static void importCsvToConidmap() {
		SqlSession session = null;
		try {
			long startTime = System.currentTimeMillis();
			
			session = MyBatisUtil.getSessionFactory().openSession(ExecutorType.BATCH,false);
			
			File conidmapFile = new File("data/conidmap.csv");
			FileInputStream fis = new FileInputStream(conidmapFile);
			InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
			BufferedReader br1 = new BufferedReader(isr);

			long fileSize = 1;
			br1.readLine();
			System.out.println(session.getConfiguration().getDefaultExecutorType());
			System.out.println(session.getConnection().getAutoCommit());
			while(br1.ready()){
				String line = br1.readLine();
				String[] splited = line.split("\\|");

				ConidMap conidmap = new ConidMap();
				conidmap.setConceptId(splited[0]);
				conidmap.setCtv3Id(splited[1]);
				conidmap.setSnomedId(splited[2]);
				conidmap.setCode(splited[3]);
				conidmap.setGid(splited[4]);
				conidmap.setExecutionId(splited[5]);
				session.insert("com.termmed.genid.data.ConidMapMapper.insertConidMap", conidmap);
				if(fileSize % 1000 == 0){
					session.commit();
					long partialEndTime = System.currentTimeMillis();
					System.out.println("1000 rows comited in " + (partialEndTime - startTime) / 1000 + " Seconds");
				}
				fileSize ++;
			}
			session.commit();
			System.out.println(fileSize);
			long endTime = System.currentTimeMillis();
			
			System.out.println(fileSize + " Rows inserted in " + ( (endTime - startTime) / 1000) + " Seconds");
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			session.close();
		}
	}
}
