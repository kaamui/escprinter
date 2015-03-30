/*
 *
Copyright (c) 2006-2007, Giovanni Martina
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that 
the following conditions are met:

- Redistributions of source code must retain the above copyright notice, this list of conditions and the 
following disclaimer.

- Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the 
following disclaimer in the documentation and/or other materials provided with the distribution.

-Neither the name of Drayah, Giovanni Martina nor the names of its contributors may be used to endorse or 
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
 * MatrixMain.java
 *
 * Created on 4 de Setembro de 2006, 16:22
 *
 * @author giovanni <gio@drayah.net>
 * Copyright © 2006 G.M. Martina
 *
 * Main class for matrix printer tests
 *
 */

/*
 *   Edited on 30/03/2015
 *   @author Clement <fc86@outlook.fr>
 *   Class that doesn't change the great work of Giovanni but add Linux Compatibility (maybe also on Mac, not tested). In Linux, use Windows Giovanni sample, and just add print() call before closing streams
 */
package net.drayah.matrixprinter;

import javax.print.PrintService;

/**
 *
 * @author clement
 */
public class MatrixMain 
{    
    private static void classicUse(String printerName)
    {
        EscPrinter escp = new EscPrinter(printerName, false);
        if (escp.initialize())
        {
            escp.select15CPI(); //15 characters per inch printing
            escp.advanceVertical(5); //go down 5cm
            escp.setAbsoluteHorizontalPosition(5); //5cm to the right
            escp.bold(true);
            escp.print("Let's print some matrix text ;)");
            escp.bold(false);
            escp.advanceVertical(1);
            escp.setAbsoluteHorizontalPosition(5); //back to 5cm on horizontal axis
            escp.print("Very simple and easy!");            
            escp.formFeed(); //eject paper

            if (!System.getProperty("os.name").toLowerCase().equalsIgnoreCase("win")) //if you don't need to support cross platform, adapt the code  
                escp.print();

            escp.close(); //close stream
        }
        else
        {
            System.out.println("could not find speicifed printer");
        }
    }        
    
    
    private static void printAnExistingFile(String printerName, String filename) 
    {
         EscPrinter escp = new EscPrinter(printerName, false);
        if (escp.initialize())
        {
            escp.printFile(filename);
        }
        else
        {
            System.out.println("could not find speicifed printer");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {  
        // These lines of code are examples of using on a linux or Windows computer
        // Be careful : The printers you're trying to access must be registered on your computer,
        // it is not possible to access to a remote printer
                
        //static method to check if a printer named "Generic-ESC-P-Dot-Matrix" is available
        if (EscPrinter.printerExists("Generic-ESC-P-Dot-Matrix"))
            System.out.println("this printer is available !");
        else
            System.out.println("could not find the specified printer");
            
        //static method to find all 2D print services available (like a service for the printer you are searching)
        for (PrintService service : EscPrinter.printServices())
            System.out.println("found a print service named : " + service.getName());
        
        //the simple and fast way provided by Giovanni, but with Linux compatibility (where you cannot access like this : "\\computername\printername")
        classicUse("Generic-ESC-P-Dot-Matrix"); //for Linux
        //classicUse("\\computername\printername"); //for Windows
        
        //another way to use the API with an exisiting file (mime-type: octet-stream) containing ESC-P or ESCP-2 sequences
        printAnExistingFile("Generic-ESC-P-Dot-Matrix", "/home/user/Documents/escp2.txt"); //for Linux (a file with ESC-P or ESC-P2 format)
        //printAnExistingFile("\\computername\printername", "C:/escp2.txt"); //for Windows (a file with ESC-P or ESC-P2 format)                   
    }    
}
