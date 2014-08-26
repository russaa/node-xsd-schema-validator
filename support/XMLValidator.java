package support;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


public class XMLValidator implements ErrorHandler {

  static final String END = "---end---";
  static final int READ_LENGTH = 256;

  private boolean withErrors = false;

  public XMLValidator() {

  }

  private static void println(PrintStream out, String msg) {
    out.println(msg);
    out.flush();
  }

  private static void println(String msg) {
    println(System.out, msg);
  }

  @Override
  public void warning(SAXParseException exception) throws SAXException {
    println(String.format("[warning] %s (%d:%d)", exception.getMessage(), exception.getLineNumber(), exception.getColumnNumber()));

    withErrors = true;
  }

  @Override
  public void error(SAXParseException exception) throws SAXException {
    println(String.format("[error] %s (%d:%d)", exception.getMessage(), exception.getLineNumber(), exception.getColumnNumber()));

    withErrors = true;
  }

  @Override
  public void fatalError(SAXParseException exception) throws SAXException {
    println(String.format("[fatal] %s (%d:%d)", exception.getMessage(), exception.getLineNumber(), exception.getColumnNumber()));

    withErrors = true;
  }

  private static Schema loadSchema(String fileName) throws Exception {
    SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema = sf.newSchema(new File(fileName));

    return schema;
  }
  
  private static boolean endsWith(StringWriter w, String s) throws IOException {
	
	StringBuffer b = w.getBuffer();
	int len  = b.length();
	int len2 = s.length();
	if(len >= len2){
		boolean isMatch = true;
		int i = len-1;
		//ignore endings with line-breaks
		while(b.charAt(i) == '\n' || b.charAt(i) == '\r') --i;
		
		for(int j=len2-1; i>=0 && j>=0; --i){
			if(b.charAt(i) != s.charAt(j)){
				isMatch = false;
				break;
			}
			--j;
		}
		return isMatch;
	}
	else 
		return false;
  }

  private static InputStream readFromSysIn() throws IOException {

    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    StringWriter writer = new StringWriter();
	
	char[] buf = new char[READ_LENGTH];
	int result = 0;
    while (result != -1) {
	  
	  result = reader.read(buf, 0, READ_LENGTH);
	  
	  if(result != -1){
		writer.write(buf, 0, result);
	  }
		
	  if(endsWith(writer, END)){
		break;
	  }
    }

    return new ByteArrayInputStream(writer.toString().getBytes());
  }

  public static void main(String[] args) throws Exception {

    boolean readStdin = false;
    String fileName = null;
    String schemaFile = null;

    for (String str: args) {
      if ("-stdin".equals(str)) {
        readStdin = true;
      } else
      if (str.startsWith("-file=")) {
        fileName = str.replaceFirst("-file=", "");
      } else
      if (str.startsWith("-schema=")) {
        schemaFile = str.replaceFirst("-schema=", "");
      }
    }

    if (schemaFile == null) {
      println(System.err, "[error] specify schema via -schema=[SCHEMA]");
      System.exit(1);
    }

    InputStream inputStream;

    if (readStdin) {
      inputStream = readFromSysIn();
    } else {
      inputStream = new FileInputStream(fileName);
    }

    XMLValidator handler = new XMLValidator();

    try {
      Schema schema = loadSchema(schemaFile);
      Validator validator = schema.newValidator();

      validator.setErrorHandler(handler);
      validator.validate(new StreamSource(inputStream));

      println("result=" + (handler.withErrors ? "WITH_ERRORS" : "OK"));

    } catch (Exception e) {
      println("[fatal] " + e.getMessage());
      println("result=FATAL_ERROR");

      handler.withErrors = true;
    }

    System.exit(handler.withErrors ? 1 : 0);
  }
}