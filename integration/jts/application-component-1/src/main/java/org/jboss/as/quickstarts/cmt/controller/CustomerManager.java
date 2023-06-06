/*
 * Copyright The Narayana Authors
 * SPDX short identifier: Apache-2.0
 */

package org.jboss.as.quickstarts.cmt.controller;

import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import org.jboss.as.quickstarts.cmt.jts.ejb.CustomerManagerEJB;

@Named("customerManager")
@RequestScoped
public class CustomerManager {
	private Logger logger = Logger.getLogger(CustomerManager.class.getName());

	@EJB
	private CustomerManagerEJB customerManager;

	private static String timeP;

	public String addCustomer() {
		try {
			Map requestMap = FacesContext.getCurrentInstance()
					.getExternalContext().getRequestParameterMap();
			int invocationCount = 1;
			if (requestMap.get("name") != null) {
				invocationCount = Integer.parseInt((String) requestMap
						.get("name"));
			}
			System.out.println("invocation count: " + invocationCount);
			long time = System.currentTimeMillis();
			System.out.println(new Date());
			for (int i = 0; i < invocationCount; i++) {
				customerManager.createCustomer(i + "");
			}
			long timeNow = System.currentTimeMillis();
			timeP = "invocationCount " + invocationCount + " took: "
					+ (timeNow - time) + " which is " + (timeNow - time)
					/ invocationCount;
			System.out.println(timeP);

			return "customerAdded";
		} catch (Exception e) {
			logger.warning("Problem: " + e.getMessage());
			e.printStackTrace();
			// Transaction will be marked rollback only anyway utx.rollback();
			return "customerDuplicate";
		}
	}

	public String getTime() {
		System.out.println("WARNING: THIS IS FROM A STATIC: " + timeP);
		return timeP;
	}
}