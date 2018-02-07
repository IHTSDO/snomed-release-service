package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.service.helper.WebSocketLogAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class WebSocketController {

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    static final Logger LOGGER = LoggerFactory.getLogger(WebSocketController.class);


    @RequestMapping(value = "/log/{name}", method = RequestMethod.GET)
    @ResponseBody
    public String testTail(@PathVariable String name) {
        org.apache.log4j.Logger logger =  org.apache.log4j.LogManager.getLogger("org.ihtsdo");
        if(logger.getAppender("websocket") == null) {
            WebSocketLogAppender webSocketLogAppender = new WebSocketLogAppender(messagingTemplate, name);
            webSocketLogAppender.setName("websocket");
            logger.addAppender(webSocketLogAppender);
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                MDC.put("trackerId", name);
                int i = 0;
                while (i < 10) {
                    try {
                        Thread.sleep(2000);
                        LOGGER.info("Test info {}", i);
                        Thread.sleep(2000);
                        LOGGER.error("Test error {}", i);
                        i++;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
        return "Test";
    }

}
