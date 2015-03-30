/*
 
Copyright (c) 2006-2007, Giovanni Martina
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that 
the following conditions are met:

- Redistributions of source code must retain the above copyright notice, this list of conditions and the 
following disclaimer.

- Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the 
following disclaimer in the documentation and/or other materials provided with the distribution.

- Neither the name of Drayah, Giovanni Martina nor the names of its contributors may be used to endorse or 
promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED 
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY 
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH 
DAMAGE.
 * 
 * ESCPrinter.java
 *
 * Created on 10 de Setembro de 2006, 13:57
 *
 * @author Giovanni <gio@drayah.net>
 * Copyright © 2006 G.M. Martina
 *
 * Class that enables printing to ESC/P and ESC/P2 dot matrix printers (e.g. Epson LQ-570, Epson LX-300) by writing directly to a stream using standard I/O
 * Like this we have direct control over the printerName and bypass Java2D printing which is considerably slower printing in graphics to a dotmatrix
 *
 */

/*
 *   Edited on 30/03/2015
 *   @author Clement <fc86@outlook.fr>
 *   Class that doesn't change the great work of Giovanni but add Linux Compatibility (maybe also on Mac, not tested). In Linux, use Windows sample, and just add print() call before closing streams
 */

package net.drayah.matrixprinter;

import java.awt.print.PrinterJob;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.SimpleDoc;
import javax.print.attribute.HashDocAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;

public class EscPrinter 
{    
    /* fields */
    private String printerName;
    private boolean escp24pin;
    private FileOutputStream ostream;
    private FileInputStream istream;
    private PrintStream pstream;
    private boolean streamOpenSuccess;
    private static final int MAX_ADVANCE_9PIN = 216; //for 24/48 pin esc/p2 printers this should be 180
    private static final int MAX_ADVANCE_24PIN = 180;
    private static final int MAX_UNITS = 127; //for vertical positioning range is between 0 - 255 (0 <= n <= 255) according to epson ref. but 255 gives weird errors at 1.5f, 127 as max (0 - 128) seems to be working
    private static final float CM_PER_INCH = 2.54f;
    
    /* decimal ascii values for epson ESC/P commands */
    private static final char ESC = 27; //escape
    private static final char AT = 64; //@
    private static final char LINE_FEED = 10; //line feed/new line
    private static final char PARENTHESIS_LEFT = 40;
    private static final char BACKSLASH = 92;
    private static final char CR = 13; //carriage return
    private static final char TAB = 9; //horizontal tab
    private static final char FF = 12; //form feed
    private static final char g = 103; //15cpi pitch
    private static final char p = 112; //used for choosing proportional mode or fixed-pitch
    private static final char t = 116; //used for character set assignment/selection
    private static final char l = 108; //used for setting left margin
    private static final char x = 120; //used for setting draft or letter quality (LQ) printing
    private static final char E = 69; //bold font on
    private static final char F = 70; //bold font off
    private static final char J = 74; //used for advancing paper vertically
    private static final char P = 80; //10cpi pitch
    private static final char Q = 81; //used for setting right margin
    private static final char $ = 36; //used for absolute horizontal positioning
    private static final char ARGUMENT_0 = 0;
    private static final char ARGUMENT_1 = 1;
    private static final char ARGUMENT_2 = 2;
    private static final char ARGUMENT_3 = 3;
    private static final char ARGUMENT_4 = 4;
    private static final char ARGUMENT_5 = 5;
    private static final char ARGUMENT_6 = 6;
    private static final char ARGUMENT_7 = 7;
    private static final char ARGUMENT_25 = 25;
    
    /* character sets */
    public static final char USA = ARGUMENT_1;
    public static final char BRAZIL = ARGUMENT_25;
    
    /** Creates a new instance of ESCPrinter
     *  @param printerName the name or location of the printerName. On windows, you can specify the {@code printerName} by using \\computername\printername. 
        In Linux, you can just give the name of the printerName as it is registered by the current user. To find all printers available, use the static method
        EscPrinter.printServices() (the name of printers are available with the method getName() of a printService instance)
     *  @param escp24pin indicates whether the printerName is a 24 pin esc/p2 epson
     */
    public EscPrinter(String printerName, boolean escp24pin) 
    {
        if (printerName == null)
            throw new IllegalArgumentException("the printer name or location cannot be null");

        this.printerName = printerName;
        this.escp24pin = escp24pin;
    }
    
    
    /**
     *  check if a printService is available for the printerName named {@code printerName}
     *  @param printerName the name of the printer
     *  @return a possibly empty array of PrintService (service of printerName).
     */
    public static boolean printerExists(String printerName)
    {
        return printService(printerName) != null;
    }
    
    /**
     *  retrieve a list of services of all registered printers for current user
     *  @return a possibly empty array of PrintService (service of printerName).
     */
    public static PrintService[] printServices()
    {               
        return PrinterJob.lookupPrintServices();
    }
    
    /**
     *  try to retrieve a PrintService of the printer named {@code printerName}
     *  @param printerName the name of the printer to find
     *  @return a PrintService of the printer printerName if found, else null.
     */
    public static PrintService printService(String printerName) 
    {        
        for (PrintService service : PrinterJob.lookupPrintServices())
        {
            if (service.getName().equalsIgnoreCase(printerName))
                return service;
        }
        
        return null;
    }
    
    /**
     *  try to retrieve a PrintService of the printer named {@code printerName}
     *  @return a PrintService of the current printer if found, else null. 
     *  @throws java.lang.Exception 
     */
    public PrinterJob createPrinterJob() throws Exception 
    {
        PrintService printService = EscPrinter.printService(printerName);

        if (printService == null)
            throw new IllegalStateException("Could not find Printer Service \"" + printerName + '"');

        PrinterJob printerJob = PrinterJob.getPrinterJob();

        printerJob.setPrintService(printService);

        return printerJob;
    }
    
    public void close() 
    {
        try 
        {
            pstream.close();
            istream.close();
            ostream.close();
        } 
        catch (IOException ex) 
        {
            Logger.getLogger(EscPrinter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public boolean initialize() 
    {
        try 
        {
            streamOpenSuccess = false;
            
            //create stream objs
            ostream = new FileOutputStream(printerName);
            istream = new FileInputStream(printerName);
            pstream = new PrintStream(ostream);
            
            //reset default settings
            pstream.print(ESC);
            pstream.print(AT);
            
            //select 10-cpi character pitch
            select10CPI();
            
            //select draft quality printing
            selectDraftPrinting();
            
            //set character set
            setCharacterSet(USA);
            streamOpenSuccess = true;
        } 
        catch (FileNotFoundException ex)
        {
            Logger.getLogger(EscPrinter.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return streamOpenSuccess;
    }
    
    public void print()
    {
        printFile(null, null);
    }
    
    public void printFile(String filename)
    {
        printFile(filename, null);
    }
    
    public void printFile(String filename, PrintRequestAttributeSet attributes)
    {
        try 
        {
            PrinterJob job = createPrinterJob();
            
            if (job != null)
            {
                Doc simpleDoc;
                if (filename == null)
                {
                    simpleDoc = new SimpleDoc(
                        istream,
                        DocFlavor.INPUT_STREAM.AUTOSENSE,
                        new HashDocAttributeSet());
                }
                else
                {
                    simpleDoc = new SimpleDoc(
                        new FileInputStream(filename),
                        DocFlavor.INPUT_STREAM.AUTOSENSE,
                        new HashDocAttributeSet());
                }
                
                job.getPrintService().createPrintJob().print(simpleDoc, attributes);               
            }
            
        } catch (Exception ex) {
            Logger.getLogger(EscPrinter.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }    
        
    public void print(String text) {
        pstream.print(text);
    }
    
    public void select10CPI() { //10 characters per inch (condensed available)
        pstream.print(ESC);
        pstream.print(P);
    }
    
    public void select15CPI() { //15 characters per inch (condensend not available)
        pstream.print(ESC);
        pstream.print(g);
    }
    
    public void selectDraftPrinting() { //set draft quality printing
        pstream.print(ESC);
        pstream.print(x);
        pstream.print((char) 48);
    }
    
    public void selectLQPrinting() { //set letter quality printing
        pstream.print(ESC);
        pstream.print(x);
        pstream.print((char) 49);
    }
    
    public void setCharacterSet(char charset) {
        //assign character table
        pstream.print(ESC);
        pstream.print(PARENTHESIS_LEFT);
        pstream.print(t);
        pstream.print(ARGUMENT_3); //always 3
        pstream.print(ARGUMENT_0); //always 0
        pstream.print(ARGUMENT_1); //selectable character table 1
        pstream.print(charset); //registered character table (arg_25 is brascii)
        pstream.print(ARGUMENT_0); //always 0
        
        //select character table
        pstream.print(ESC);
        pstream.print(t);
        pstream.print(ARGUMENT_1); //selectable character table 1
    }
    
    public void lineFeed() {
        //post: performs new line
        pstream.print(CR); //according to epson esc/p ref. manual always send carriage return before line feed
        pstream.print(LINE_FEED);
    }
    
    public void formFeed() {
        //post: ejects single sheet
        pstream.print(CR); //according to epson esc/p ref. manual it is recommended to send carriage return before form feed
        pstream.print(FF);
    }
    
    public void bold(boolean bold) {
        pstream.print(ESC);
        if (bold)
            pstream.print(E);
        else
            pstream.print(F);
    }
    
    public void proportionalMode(boolean proportional) {
        pstream.print(ESC);
        pstream.print(p);
        if (proportional)
            pstream.print((char) 49);
        else
            pstream.print((char) 48);
    }
    
    public void advanceVertical(float centimeters) {
        //pre: centimeters >= 0 (cm)
        //post: advances vertical print position approx. y centimeters (not precise due to truncation)
        float inches = centimeters / CM_PER_INCH;
        int units = (int) (inches * (escp24pin ? MAX_ADVANCE_24PIN : MAX_ADVANCE_9PIN));
        
        while (units > 0) {
            char n;
            if (units > MAX_UNITS)
                n = (char) MAX_UNITS; //want to move more than range of parameter allows (0 - 255) so move max amount
            else
                n = (char) units; //want to move a distance which fits in range of parameter (0 - 255)
                        
            pstream.print(ESC);
            pstream.print(J);
            pstream.print(n);
            
            units -= MAX_UNITS;
        }
    }
    
    public void advanceHorizontal(float centimeters) {
        //pre: centimeters >= 0
        //post: advances horizontal print position approx. centimeters
        float inches = centimeters / CM_PER_INCH;
        int units_low = (int) (inches * 120) % 256;
        int units_high = (int) (inches * 120) / 256;
        
        pstream.print(ESC);       
        pstream.print(BACKSLASH);
        pstream.print((char) units_low);
        pstream.print((char) units_high);
    }
    
    public void setAbsoluteHorizontalPosition(float centimeters) {
        //pre: centimenters >= 0 (cm)
        //post: sets absolute horizontal print position to x centimeters from left margin
        float inches = centimeters / CM_PER_INCH;
        int units_low = (int) (inches * 60) % 256;
        int units_high = (int) (inches * 60) / 256;
        
        pstream.print(ESC);
        pstream.print($);
        pstream.print((char) units_low);
        pstream.print((char) units_high);
    }
    
    public void horizontalTab(int tabs) {
        //pre: tabs >= 0
        //post: performs horizontal tabs tabs number of times
        for (int i = 0; i < tabs; i++)
            pstream.print(TAB);
    }
    
    public void setMargins(int columnsLeft, int columnsRight) {
        //pre: columnsLeft > 0 && <= 255, columnsRight > 0 && <= 255
        //post: sets left margin to columnsLeft columns and right margin to columnsRight columns
        //left
        pstream.print(ESC);
        pstream.print(l);
        pstream.print((char) columnsLeft);
        
        //right
        pstream.print(ESC);
        pstream.print(Q);
        pstream.print((char) columnsRight);
    }
    
    public boolean isInitialized() {
        //post: returns true iff printerName was successfully initialized
        return streamOpenSuccess;
    }
    
    public String getShare() {
        //post: returns printerName share name (Windows network)
        return printerName;
    }
    
    @Override
    public String toString() {
        //post: returns String representation of ESCPrinter e.g. <ESCPrinter[share=...]>
        StringBuilder strb = new StringBuilder();
        strb.append("<ESCPrinter[share=").append(printerName).append(", 24pin=").append(escp24pin).append("]>");
        return strb.toString();
    }    
}
