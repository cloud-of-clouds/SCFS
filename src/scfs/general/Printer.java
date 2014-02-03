package scfs.general;

public class Printer {

	private static boolean printAuth;
	
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

	public static void println(Object output){
		if(printAuth){
			System.out.println(output);
			System.out.flush();
		}
	}
	
	public static void println(Object output, String cor){
		if(printAuth){
			
			if(cor.equals("preto")){
				System.out.println(ANSI_BLACK + output);
			}else if(cor.equals("branco")){
				System.out.println(ANSI_WHITE + output);
			}else if(cor.equals("ciao")){
				System.out.println(ANSI_CYAN + output);
			}else if(cor.equals("amarelo")){
				System.out.println(ANSI_YELLOW + output);
			}else if(cor.equals("verde")){
				System.out.println(ANSI_GREEN + output);
			}else if(cor.equals("vermelho")){
				System.out.println(ANSI_RED + output);
			}else if(cor.equals("roxo")){
				System.out.println(ANSI_PURPLE + output);
			}else if(cor.equals("azul")){
				System.out.println(ANSI_CYAN + output);
			}else if(cor.equals("Dazul")){
				System.out.println(ANSI_BLUE + output);
			}
			System.out.print(ANSI_RESET);
			System.out.flush();
		}
	}

	public static void print(Object output){
		if(printAuth){
			System.out.println(output);
			System.out.flush();
		}
	}

	public static void printlnErr(Object output){
		if(printAuth){
			System.err.println(output);
			System.out.flush();
		}
	}

	public static void setPrintAuth(boolean printAuthNew){
		printAuth = printAuthNew;

	}

}
