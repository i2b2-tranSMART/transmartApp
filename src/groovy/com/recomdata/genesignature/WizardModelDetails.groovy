/**
 * model details class for the create/edit wizard in the gene signature module
 */
package com.recomdata.genesignature

import com.recomdata.util.ModelDetails
import org.transmart.biomart.ConceptCode

/**
 * @author jspencer
 */
class WizardModelDetails extends ModelDetails {

	// wizard tyes
	static final int WIZ_TYPE_CREATE = 0
	static final int WIZ_TYPE_EDIT = 1
	static final int WIZ_TYPE_CLONE = 2

	// default is create
	int wizardType = WIZ_TYPE_CREATE

	// pick lists
	def sources
	def owners
	def species
	def mouseSources
	def tissueTypes
	def expTypes
	def analyticTypes
	def normMethods
	def analysisMethods
	def schemas
	def pValCutoffs
	def foldChgMetrics
	def platforms
	def compounds

	// domain class
	def geneSigInst

	// id of domain being edited
	def editId
	def cloneId

	/**
	 * add an empty other ConceptCode item
	 */
	static void addOtherItem(List<ConceptCode> items, String optionId) {
		items << new ConceptCode(bioConceptCode: optionId ?: 'other', codeName: 'other')
	}
}
