escprinter
=======

Simple Java class to allow printing to Epson ESC/P and ESC/P2 matrix printers. See below for the different ways to use it. 
You can also try to compile and run MatrixMain.java to test these features 
            
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

		    **if (!System.getProperty("os.name").toLowerCase().equalsIgnoreCase("win")) //if you don't need to support cross platform, adapt the code**  
		        **escp.print();**

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
