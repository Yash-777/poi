/*
 *  ====================================================================
 *    Licensed to the Apache Software Foundation (ASF) under one or more
 *    contributor license agreements.  See the NOTICE file distributed with
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with
 *    the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * ====================================================================
 */

package org.apache.poi.ss.formula;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.atp.AnalysisToolPak;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.FunctionEval;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.usermodel.CellValue;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFunctionRegistry {

    HSSFWorkbook wb;
    HSSFSheet sheet;
    HSSFRow row;
    HSSFFormulaEvaluator fe;

    @Before
    public void setup() {
        wb = new HSSFWorkbook();
        sheet = wb.createSheet("Sheet1");
        row = sheet.createRow(0);
        fe = new HSSFFormulaEvaluator(wb);
    }

    @After
    public void teardown() throws IOException {
        wb.close();
        wb = null;
        sheet = null;
        row = null;
        fe = null;
    }

    @Test(expected = NotImplementedException.class)
	public void testRegisterInRuntimeA() {
        HSSFCell cellA = row.createCell(0);
        cellA.setCellFormula("FISHER(A5)");
        fe.evaluate(cellA);
    }

    @Test
    public void testRegisterInRuntimeB() {
        HSSFCell cellA = row.createCell(0);
        cellA.setCellFormula("FISHER(A5)");
        FunctionEval.registerFunction("FISHER", (args, srcRowIndex, srcColumnIndex) -> ErrorEval.NA);
        CellValue cv = fe.evaluate(cellA);
        assertEquals(ErrorEval.NA.getErrorCode(), cv.getErrorValue());
    }

    @Test(expected = NotImplementedException.class)
    public void testRegisterInRuntimeC() {
        HSSFCell cellB = row.createCell(1);
        cellB.setCellFormula("CUBEMEMBERPROPERTY(A5)");
        fe.evaluate(cellB);
    }

    @Test
    public void testRegisterInRuntimeD() {
        HSSFCell cellB = row.createCell(1);
        cellB.setCellFormula("CUBEMEMBERPROPERTY(A5)");

        AnalysisToolPak.registerFunction("CUBEMEMBERPROPERTY", (args, ec) -> ErrorEval.NUM_ERROR);

        CellValue cv = fe.evaluate(cellB);
        assertEquals(ErrorEval.NUM_ERROR.getErrorCode(), cv.getErrorValue());
	}

	private static ValueEval na(ValueEval[] args, int srcRowIndex, int srcColumnIndex) {
        return ErrorEval.NA;
    }

    @Test
    public void testExceptionsA() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> FunctionEval.registerFunction("SUM", TestFunctionRegistry::na)
        );
        assertEquals("POI already implements SUM. You cannot override POI's implementations of Excel functions", ex.getMessage());
    }

    @Test
    public void testExceptionsB() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> FunctionEval.registerFunction("SUMXXX", TestFunctionRegistry::na)
        );
        assertTrue(ex.getMessage().contains("Unknown function: SUMXXX"));
    }

    @Test
    public void testExceptionsC() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> FunctionEval.registerFunction("ISODD", TestFunctionRegistry::na)
        );
        assertEquals("ISODD is a function from the Excel Analysis Toolpack. " +
             "Use AnalysisToolpack.registerFunction(String name, FreeRefFunction func) instead.",
             ex.getMessage());
    }

    private static ValueEval atpFunc(ValueEval[] args, OperationEvaluationContext ec) {
        return ErrorEval.NUM_ERROR;
    }

    @Test
    public void testExceptionsD() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> AnalysisToolPak.registerFunction("ISODD", TestFunctionRegistry::atpFunc)
        );
        assertEquals("POI already implements ISODD. You cannot override POI's implementations of Excel functions", ex.getMessage());
    }

    @Test
    public void testExceptionsE() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> AnalysisToolPak.registerFunction("ISODDXXX", TestFunctionRegistry::atpFunc)
        );
        assertEquals("ISODDXXX is not a function from the Excel Analysis Toolpack.", ex.getMessage());
    }

    @Test
    public void testExceptionsF() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> AnalysisToolPak.registerFunction("SUM", TestFunctionRegistry::atpFunc)
        );
        assertEquals("SUM is a built-in Excel function. " +
             "Use FunctoinEval.registerFunction(String name, Function func) instead.",
             ex.getMessage());
    }
}
