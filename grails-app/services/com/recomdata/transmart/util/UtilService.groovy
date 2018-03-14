package com.recomdata.transmart.util

import groovy.transform.CompileStatic

@CompileStatic
class UtilService {

	static transactional = false

	String toListString(List objList) {
		StringBuilder objToString = new StringBuilder()
		for (obj in objList) {
			if (obj && obj?.toString()?.trim()) {
				if (obj instanceof String) {
					objToString << "'" << obj << "'"
				}
				else {
					objToString << obj
				}
				if (objToString) {
					objToString << ','
				}
			}
		}

		objToString
	}

	String getActualPatientId(String sourceSystemCode) {
		sourceSystemCode.split(':')[-1]
	}
}
