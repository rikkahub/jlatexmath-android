package org.scilab.forge.jlatexmath;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ChemFormulaConverterTest {

    @Test
    public void convertsSimpleMolecule() {
        assertEquals("\\mathrm{H}_{2}\\mathrm{O}", ChemFormulaConverter.convert("H2O"));
    }

    @Test
    public void convertsReaction() {
        assertEquals(
                "\\mathrm{CO}_{2}\\ \\mathbin{+}\\ \\mathrm{H}_{2}\\mathrm{O}\\ \\longrightarrow\\ \\mathrm{H}_{2}\\mathrm{CO}_{3}\\ ",
                ChemFormulaConverter.convert("CO2 + H2O -> H2CO3"));
    }

    @Test
    public void convertsCharge() {
        assertEquals("\\mathrm{SO}_{4}^{2-}", ChemFormulaConverter.convert("SO4^2-"));
        assertEquals("\\mathrm{NH}_{4}^{+}", ChemFormulaConverter.convert("NH4+"));
    }

    @Test
    public void convertsMathJaxMhchemSyntax() {
        assertEquals("\\frac{1}{2}\\,\\mathrm{H}_{2}\\mathrm{O}", ChemFormulaConverter.convert("(1/2)H2O"));
        assertEquals("\\mathrm{OH}^{-}\\mathrm{(aq)}", ChemFormulaConverter.convert("OH-(aq)"));
        assertEquals("\\mathrm{X}_{\\alpha}", ChemFormulaConverter.convert("X_\\alpha"));
        assertEquals("\\mathrm{A}\\ {\\equiv}\\ \\mathrm{B}", ChemFormulaConverter.convert("A \\bond{3} B"));
        assertEquals("\\mathrm{A}\\ \\xrightarrow[\\mathrm{k}_{-1}]{\\mathrm{H}_{2}\\mathrm{O}}\\ \\mathrm{B}",
                ChemFormulaConverter.convert("A ->[H2O][k_{-1}] B"));
        assertEquals("\\mathrm{A}\\ \\longrightleftharpoons\\ \\mathrm{B}",
                ChemFormulaConverter.convert("A <=>> B"));
        assertEquals("\\mathrm{A}\\ \\longleftrightharpoons\\ \\mathrm{B}",
                ChemFormulaConverter.convert("A <<=> B"));
    }
}
