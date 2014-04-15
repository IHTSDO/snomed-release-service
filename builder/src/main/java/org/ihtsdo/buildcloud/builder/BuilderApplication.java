package org.ihtsdo.buildcloud.builder;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class BuilderApplication {

	public static void main(String[] args) {
		new ClassPathXmlApplicationContext("applicationContext.xml");
	}

}
