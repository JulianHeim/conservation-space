package org.activiti.engine.impl.bpmn.behavior;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;

// TODO: Auto-generated Javadoc
/**
 * The Class ShellActivityBehavior.
 */
public class ShellActivityBehavior extends AbstractBpmnActivityBehavior {

  /** The command. */
  protected Expression command;
  
  /** The wait. */
  protected Expression wait;
  
  /** The arg1. */
  protected Expression arg1;
  
  /** The arg2. */
  protected Expression arg2;
  
  /** The arg3. */
  protected Expression arg3;
  
  /** The arg4. */
  protected Expression arg4;
  
  /** The arg5. */
  protected Expression arg5;
  
  /** The output variable. */
  protected Expression outputVariable;
  
  /** The error code variable. */
  protected Expression errorCodeVariable;
  
  /** The redirect error. */
  protected Expression redirectError;
  
  /** The clean env. */
  protected Expression cleanEnv;
  
  /** The directory. */
  protected Expression directory;

  /** The command str. */
  String commandStr;
  
  /** The arg1 str. */
  String arg1Str;
  
  /** The arg2 str. */
  String arg2Str;
  
  /** The arg3 str. */
  String arg3Str;
  
  /** The arg4 str. */
  String arg4Str;
  
  /** The arg5 str. */
  String arg5Str;
  
  /** The wait str. */
  String waitStr;
  
  /** The result variable str. */
  String resultVariableStr;
  
  /** The error code variable str. */
  String errorCodeVariableStr;
  
  /** The wait flag. */
  Boolean waitFlag;
  
  /** The redirect error flag. */
  Boolean redirectErrorFlag;
  
  /** The clean env boolan. */
  Boolean cleanEnvBoolan;
  
  /** The directory str. */
  String directoryStr;

  /**
   * Read fields.
   *
   * @param execution the execution
   */
  private void readFields(ActivityExecution execution) {
    commandStr = getStringFromField(command, execution);
    arg1Str = getStringFromField(arg1, execution);
    arg2Str = getStringFromField(arg2, execution);
    arg3Str = getStringFromField(arg3, execution);
    arg4Str = getStringFromField(arg4, execution);
    arg5Str = getStringFromField(arg5, execution);
    waitStr = getStringFromField(wait, execution);
    resultVariableStr = getStringFromField(outputVariable, execution);
    errorCodeVariableStr = getStringFromField(errorCodeVariable, execution);

    String redirectErrorStr = getStringFromField(redirectError, execution);
    String cleanEnvStr = getStringFromField(cleanEnv, execution);

    waitFlag = waitStr == null || waitStr.equals("true");
    redirectErrorFlag = redirectErrorStr != null && redirectErrorStr.equals("true");
    cleanEnvBoolan = cleanEnvStr != null && cleanEnvStr.equals("true");
    directoryStr = getStringFromField(directory, execution);

  }

  /* (non-Javadoc)
   * @see org.activiti.engine.impl.bpmn.behavior.FlowNodeActivityBehavior#execute(org.activiti.engine.impl.pvm.delegate.ActivityExecution)
   */
  public void execute(ActivityExecution execution) {

    readFields(execution);

    List<String> argList = new ArrayList<String>();
    argList.add(commandStr);

    if (arg1Str != null)
      argList.add(arg1Str);
    if (arg2Str != null)
      argList.add(arg2Str);
    if (arg3Str != null)
      argList.add(arg3Str);
    if (arg4Str != null)
      argList.add(arg4Str);
    if (arg5Str != null)
      argList.add(arg5Str);

    ProcessBuilder processBuilder = new ProcessBuilder(argList);

    try {
      processBuilder.redirectErrorStream(redirectErrorFlag);
      if (cleanEnvBoolan) {
        Map<String, String> env = processBuilder.environment();
        env.clear();
      }
      if (directoryStr != null && directoryStr.length() > 0)
        processBuilder.directory(new File(directoryStr));

      Process process = processBuilder.start();

      if (waitFlag) {
        int errorCode = process.waitFor();

        if (resultVariableStr != null) {
          String result = convertStreamToStr(process.getInputStream());
          execution.setVariable(resultVariableStr, result);
        }

        if (errorCodeVariableStr != null) {
          execution.setVariable(errorCodeVariableStr, Integer.toString(errorCode));

        }

      }
    } catch (Exception e) {
      throw new ActivitiException("Could not execute shell command ", e);
    }

    leave(execution);
  }

  /**
   * Convert stream to str.
   *
   * @param is the is
   * @return the string
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static String convertStreamToStr(InputStream is) throws IOException {

    if (is != null) {
      Writer writer = new StringWriter();

      char[] buffer = new char[1024];
      try {
        Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        int n;
        while ((n = reader.read(buffer)) != -1) {
          writer.write(buffer, 0, n);
        }
      } finally {
        is.close();
      }
      return writer.toString();
    } else {
      return "";
    }
  }

  /**
   * Gets the string from field.
   *
   * @param expression the expression
   * @param execution the execution
   * @return the string from field
   */
  protected String getStringFromField(Expression expression, DelegateExecution execution) {
    if (expression != null) {
      Object value = expression.getValue(execution);
      if (value != null) {
        return value.toString();
      }
    }
    return null;
  }

}
