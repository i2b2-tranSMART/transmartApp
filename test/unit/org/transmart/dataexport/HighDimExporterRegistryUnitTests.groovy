package org.transmart.dataexport

import org.gmock.WithGMock
import org.junit.Test
import org.transmartproject.export.HighDimExporter
import org.transmartproject.export.HighDimExporterRegistry

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.contains
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.hasSize

@WithGMock
class HighDimExporterRegistryUnitTests {

    HighDimExporterRegistry testee = new HighDimExporterRegistry()

    def fooExporter = [ getFormat: {-> 'foo'},
                        isDataTypeSupported: { dt -> dt == 'foo' } ] as HighDimExporter
    def barExporter = [ getFormat: {-> 'bar'},
                        isDataTypeSupported: { dt -> dt == 'bar' } ] as HighDimExporter

    @Test
    void testMultipleExportersForTheSameFileFormat() {
        def fileFormat = 'TSV'

        testee.registerHighDimensionExporter(fileFormat, fooExporter)
        testee.registerHighDimensionExporter(fileFormat, barExporter)

        def exporters = testee.findExporters(fileFormat, null)

        assertThat exporters, containsInAnyOrder(fooExporter, barExporter)
    }

    @Test
    void testSeveralFileFormatsForDataType() {
        def fooExporter2 = [ getFormat: {-> 'foo'},
                             isDataTypeSupported: { dt -> dt == 'foo' } ] as HighDimExporter

        testee.registerHighDimensionExporter('TSV', fooExporter)
        testee.registerHighDimensionExporter('CSV', barExporter)
        testee.registerHighDimensionExporter('TXT', fooExporter2)

        def exporters = testee.findExporters(null, 'foo')

        assertThat exporters, containsInAnyOrder(fooExporter, fooExporter2)
    }

    @Test
    void testAllOnNoConstraints() {
        testee.registerHighDimensionExporter('TSV', fooExporter)
        testee.registerHighDimensionExporter('CSV', barExporter)

        def exporters = testee.findExporters(null, null)

        assertThat exporters, containsInAnyOrder(fooExporter, barExporter)
    }

    @Test
    void testBothConstraints() {
        testee.registerHighDimensionExporter('TSV', fooExporter)
        testee.registerHighDimensionExporter('CSV', barExporter)

        def exporters = testee.findExporters('TSV', 'foo')

        assertThat exporters, contains(fooExporter)
    }

    @Test
    void testDoesntMatch() {
        testee.registerHighDimensionExporter('TSV', fooExporter)
        testee.registerHighDimensionExporter('CSV', barExporter)

        def exporters = testee.findExporters('CSV', 'foo')

        assertThat exporters, hasSize(0)
    }

}
