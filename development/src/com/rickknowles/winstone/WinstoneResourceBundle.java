
// Copyright (c) 2003
package com.rickknowles.winstone;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * A ResourceBundle that includes the ability to do string replacement on the
 * resources it retrieves.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 */
public class WinstoneResourceBundle
{
  private ResourceBundle resources;

  /**
   * Constructor
   */
  public WinstoneResourceBundle(String baseName)
    {this.resources = ResourceBundle.getBundle(baseName);}
  public WinstoneResourceBundle(String baseName, Locale loc)
    {this.resources = ResourceBundle.getBundle(baseName, loc);}
  public WinstoneResourceBundle(String baseName, Locale loc, ClassLoader cl)
    {this.resources = ResourceBundle.getBundle(baseName, loc, cl);}

  /**
   * Default getString method
   */
  public String getString(String key) {return this.resources.getString(key);}

  /**
   * Perform a string replace for a single from/to pair.
   */
  public String getString(String key, String fromMarker, String toValue)
    {return globalReplace(this.resources.getString(key), fromMarker, toValue);}

  /**
   * Perform a string replace for a double from/to pair.
   */
  public String getString(String key, String fromMarker1, String toValue1,
                                      String fromMarker2, String toValue2)
  {
    Map replaceParams = new HashMap();
    replaceParams.put(fromMarker1, toValue1);
    replaceParams.put(fromMarker2, toValue2);
    return getString(key, replaceParams);
  }

  private String globalReplace(String input, String fromMarker, String toValue)
  {
    if (input == null)
      return null;
    else if (fromMarker == null)
      return input;

    int fromPos = input.indexOf(fromMarker);
    if (fromPos == -1)
      return input;
    else
      return globalReplace(input.substring(0, fromPos), fromMarker, toValue) +
             toValue +
             globalReplace(input.substring(fromPos + fromMarker.length()), fromMarker, toValue);
  }

  /**
   * Perform a string replace for all the from/to pairs in the replaceParams map
   */
  public String getString(String key, Map replaceParams)
  {
    String myCopy = this.resources.getString(key);
    for (Iterator i = replaceParams.keySet().iterator(); i.hasNext(); )
    {
      String from = (String) i.next();
      String to = (String) replaceParams.get(from);
      myCopy = globalReplace(myCopy, from, to);
    }
    return myCopy;
  }

}

 