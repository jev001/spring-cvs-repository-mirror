/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.tags.form;

import org.springframework.util.ObjectUtils;

import javax.servlet.jsp.JspException;

/**
 * Base class for databinding-aware JSP tags that render HTML element. Provides
 * a set of properties corresponding to the set of HTML attributes that are common
 * across elements.
 * 
 * @author Rob Harrop
 * @since 2.0
 */
public abstract class AbstractHtmlElementTag extends AbstractDataBoundFormElementTag {

	/**
	 * The name of the '<code>class</code>' attribute.
	 */
	public static final String CLASS_ATTRIBUTE = "class";

	/**
	 * The name of the '<code>style</code>' attribute.
	 */
	public static final String STYLE_ATTRIBUTE = "style";

	/**
	 * The name of the '<code>lang</code>' attribute.
	 */
	public static final String LANG_ATTRIBUTE = "lang";

	/**
	 * The name of the '<code>title</code>' attribute.
	 */
	public static final String TITLE_ATTRIBUTE = "title";

	/**
	 * The name of the '<code>dir</code>' attribute.
	 */
	public static final String DIR_ATTRIBUTE = "dir";

	/**
	 * The name of the '<code>tabindex</code>' attribute.
	 */
	public static final String TABINDEX_ATTRIBUTE = "tabindex";

	/**
	 * The name of the '<code>onclick</code>' attribute.
	 */
	public static final String ONCLICK_ATTRIBUTE = "onclick";

	/**
	 * The name of the '<code>ondblclick</code>' attribute.
	 */
	public static final String ONDBLCLICK_ATTRIBUTE = "ondblclick";

	/**
	 * The name of the '<code>onmousedown</code>' attribute.
	 */
	public static final String ONMOUSEDOWN_ATTRIBUTE = "onmousedown";

	/**
	 * The name of the '<code>onmouseup</code>' attribute.
	 */
	public static final String ONMOUSEUP_ATTRIBUTE = "onmouseup";

	/**
	 * The name of the '<code>onmouseover</code>' attribute.
	 */
	public static final String ONMOUSEOVER_ATTRIBUTE = "onmouseover";

	/**
	 * The name of the '<code>onmousemove</code>' attribute.
	 */
	public static final String ONMOUSEMOVE_ATTRIBUTE = "onmousemove";

	/**
	 * The name of the '<code>onmouseout</code>' attribute.
	 */
	public static final String ONMOUSEOUT_ATTRIBUTE = "onmouseout";

	/**
	 * The name of the '<code>onkeypress</code>' attribute.
	 */
	public static final String ONKEYPRESS_ATTRIBUTE = "onkeypress";

	/**
	 * The name of the '<code>onkeyup</code>' attribute.
	 */
	public static final String ONKEYUP_ATTRIBUTE = "onkeyup";

	/**
	 * The name of the '<code>onkeydown</code>' attribute.
	 */
	public static final String ONKEYDOWN_ATTRIBUTE = "onkeydown";

	/**
	 * The value of the '<code>class</code>' attribute.
	 */
	private String cssClass;

	/**
	 * The value of the '<code>style</code>' attribute.
	 */
	private String cssStyle;

	/**
	 * The value of the '<code>lang</code>' attribute.
	 */
	private String lang;

	/**
	 * The value of the '<code>title</code>' attribute.
	 */
	private String title;

	/**
	 * The value of the '<code>dir</code>' attribute.
	 */
	private String dir;

	/**
	 * The value of the '<code>tabindex</code>' attribute.
	 */
	private String tabindex;

	/**
	 * The value of the '<code>onclick</code>' attribute.
	 */
	private String onclick;

	/**
	 * The value of the '<code>ondblclick</code>' attribute.
	 */
	private String ondblclick;

	/**
	 * The value of the '<code>onmousedown</code>' attribute.
	 */
	private String onmousedown;

	/**
	 * The value of the '<code>onmouseup</code>' attribute.
	 */
	private String onmouseup;

	/**
	 * The value of the '<code>onmouseover</code>' attribute.
	 */
	private String onmouseover;

	/**
	 * The value of the '<code>onmousemove</code>' attribute.
	 */
	private String onmousemove;

	/**
	 * The value of the '<code>onmouseout</code>' attribute.
	 */
	private String onmouseout;

	/**
	 * The value of the '<code>onkeypress</code>' attribute.
	 */
	private String onkeypress;

	/**
	 * The value of the '<code>onkeyup</code>' attribute.
	 */
	private String onkeyup;

	/**
	 * The value of the '<code>onkeydown</code>' attribute.
	 */
	private String onkeydown;

	/**
	 * Sets the value of the '<code>class</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setCssClass(String cssClass) {
		this.cssClass = cssClass;
	}

	/**
	 * Sets the value of the '<code>style</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setCssStyle(String cssStyle) {
		this.cssStyle = cssStyle;
	}

	/**
	 * Sets the value of the '<code>lang</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setLang(String lang) {
		this.lang = lang;
	}

	/**
	 * Sets the value of the '<code>title</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Sets the value of the '<code>dir</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setDir(String dir) {
		this.dir = dir;
	}

	/**
	 * Sets the value of the '<code>tabindex</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setTabindex(String tabindex) {
		this.tabindex = tabindex;
	}

	/**
	 * Sets the value of the '<code>onclick</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setOnclick(String onclick) {
		this.onclick = onclick;
	}

	/**
	 * Sets the value of the '<code>ondblclick</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setOndblclick(String ondblclick) {
		this.ondblclick = ondblclick;
	}

	/**
	 * Sets the value of the '<code>onmousedown</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setOnmousedown(String onmousedown) {
		this.onmousedown = onmousedown;
	}

	/**
	 * Sets the value of the '<code>onmouseup</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setOnmouseup(String onmouseup) {
		this.onmouseup = onmouseup;
	}

	/**
	 * Sets the value of the '<code>onmouseover</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setOnmouseover(String onmouseover) {
		this.onmouseover = onmouseover;
	}

	/**
	 * Sets the value of the '<code>onmousemove</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setOnmousemove(String onmousemove) {
		this.onmousemove = onmousemove;
	}

	/**
	 * Sets the value of the '<code>onmouseout</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setOnmouseout(String onmouseout) {
		this.onmouseout = onmouseout;
	}

	/**
	 * Sets the value of the '<code>onkeypress</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setOnkeypress(String onkeypress) {
		this.onkeypress = onkeypress;
	}

	/**
	 * Sets the value of the '<code>onkeyup</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setOnkeyup(String onkeyup) {
		this.onkeyup = onkeyup;
	}

	/**
	 * Sets the value of the '<code>onkeydown</code>' attribute.
	 * May be a runtime expression.
	 */
	public void setOnkeydown(String onkeydown) {
		this.onkeydown = onkeydown;
	}

	/**
	 * Writes the default attributes configured via this base class to the supplied {@link TagWriter}.
	 * Subclasses should call this when they want the base attribute set to be written to the output.
	 */
	protected void writeDefaultAttributes(TagWriter tagWriter) throws JspException {
		super.writeDefaultAttributes(tagWriter);
		tagWriter.writeOptionalAttributeValue(CLASS_ATTRIBUTE, ObjectUtils.nullSafeToString(evaluate("cssClass", this.cssClass)));
		tagWriter.writeOptionalAttributeValue(STYLE_ATTRIBUTE, ObjectUtils.nullSafeToString(evaluate("cssStyle", this.cssStyle)));
		writeOptionalAttribute(tagWriter, LANG_ATTRIBUTE, this.lang);
		writeOptionalAttribute(tagWriter, TITLE_ATTRIBUTE, this.title);
		writeOptionalAttribute(tagWriter, DIR_ATTRIBUTE, this.dir);
		writeOptionalAttribute(tagWriter, TABINDEX_ATTRIBUTE, this.tabindex);
		writeOptionalAttribute(tagWriter, ONCLICK_ATTRIBUTE, this.onclick);
		writeOptionalAttribute(tagWriter, ONDBLCLICK_ATTRIBUTE, this.ondblclick);
		writeOptionalAttribute(tagWriter, ONMOUSEDOWN_ATTRIBUTE, this.onmousedown);
		writeOptionalAttribute(tagWriter, ONMOUSEUP_ATTRIBUTE, this.onmouseup);
		writeOptionalAttribute(tagWriter, ONMOUSEOVER_ATTRIBUTE, this.onmouseover);
		writeOptionalAttribute(tagWriter, ONMOUSEMOVE_ATTRIBUTE, this.onmousemove);
		writeOptionalAttribute(tagWriter, ONMOUSEOUT_ATTRIBUTE, this.onmouseout);
		writeOptionalAttribute(tagWriter, ONKEYPRESS_ATTRIBUTE, this.onkeypress);
		writeOptionalAttribute(tagWriter, ONKEYUP_ATTRIBUTE, this.onkeyup);
		writeOptionalAttribute(tagWriter, ONKEYDOWN_ATTRIBUTE, this.onkeydown);
	}


}
