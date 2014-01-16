package org.ihtsdo.buildcloud.controller;

import org.ihtsdo.buildcloud.entity.ReleaseCentre;
import org.ihtsdo.buildcloud.service.ReleaseCentreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class ReleaseCentreController {

	@Autowired
	private ReleaseCentreService service;

	@RequestMapping("/release-centres")
	public @ResponseBody List<ReleaseCentre> getReleaseCentres() {
		return service.getReleaseCentres();
	}

}
