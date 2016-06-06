package hudson.plugins.collabnet.util;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebAssert;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlLabel;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.model.AbstractProject;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Iterator;
import java.util.List;

import org.jenkinsci.remoting.RoleChecker;
import org.jvnet.hudson.test.HudsonTestCase.WebClient;

/**
 * Class for generally useful test methods.
 */
public class Util {

    private Util() {}

    /**
     * Get the first HtmlElement by name.  Returns null if no HtmlElements
     * with that name are found.
     */
    public static HtmlElement getFirstHtmlElementByName(HtmlPage page, 
                                                        String name) {
        List<HtmlElement> elements = page.getElementsByName(name);
        if (elements.isEmpty()) {
            return null;
        } else {
            return elements.get(0);
        }
    }

    /**
     * Get the HtmlElement with a given name that has a matching label.
     * In the case of multiple matches, it will return only the first.
     * Returns null if no match is found.
     */
    public static HtmlElement getElementWithLabel(HtmlPage page, String name, 
                                                  String label) {
        List<HtmlElement> elems = page.getElementsByName(name);
        HtmlElement match = null;
        for (HtmlElement elem: elems) {
            String elemLabel = Util.findLabel(elem);
            if (label.matches(elemLabel)) {
                match = elem;
                break;
            }
        }
        return match;
    } 

    /**
     * Return the first HtmlForm that has a given action.  Returns null
     * if no such form is found.
     */
    public static HtmlForm getFormWithAction(HtmlPage page, String action) {
        List<HtmlForm> forms = page.getForms();
        HtmlForm submitForm = null;
        for(HtmlForm form: forms) {
            if (action.equals(form.getActionAttribute())) {
                submitForm = form;
                break;
            }
        }
        return submitForm;
    }    

    /**
     * Find the String label for a given node.  Assumes that the Label will be
     * a sibling.  If no label is found, returns null.
     */
    public static String findLabel(DomNode node) {
        Iterable<DomNode> siblings = node.getParentNode().getChildren();
        for (DomNode sibling : siblings) {
            if (sibling instanceof HtmlLabel) {
                HtmlLabel label = (HtmlLabel) sibling;
                if (label.getReferencedElement().equals(node)) {
                    return label.getTextContent();
                }
            }
        }
        return null;
    }

    /**
     * Find a password box on the page and set the text.
     */
    public static Page setPassword(HtmlPage page, String id, String text) {
        WebAssert.assertElementPresent(page, id);
        HtmlElement elem = page.getElementById(id);
        assert(elem instanceof HtmlPasswordInput);
        return ((HtmlPasswordInput) elem).setValueAttribute(text);
    }

    /**
     * Verify that the text in the password box is as expected.
     */
    public static void checkPassword(HtmlPage page, String id, String text) {
        WebAssert.assertElementPresent(page, id);
        HtmlElement elem = page.getElementById(id);
        assert(elem instanceof HtmlPasswordInput);
        assert(text.equals(((HtmlPasswordInput) elem).getValueAttribute()));
    }

    /**
     * Find a text box on the page and set the text.
     */
    public static Page setText(HtmlPage page, String id, String text) {
        WebAssert.assertElementPresent(page, id);
        HtmlElement elem = page.getElementById(id);
        assert(elem instanceof HtmlTextInput);
        return ((HtmlTextInput) elem).setValueAttribute(text);
    }

    /**
     * Verify that the text in the text box is as expected.
     */
    public static void checkText(HtmlPage page, String id, String text) {
        WebAssert.assertElementPresent(page, id);
        HtmlElement elem = page.getElementById(id);
        assert(elem instanceof HtmlTextInput);
        assert(text.equals(((HtmlTextInput) elem).getValueAttribute()));
    }

    /**
     * Find a text box on the page and set the text in the first element
     * with the given name.
     */
    public static Page setTextByName(HtmlPage page, String name, String text) {
        HtmlElement elem = getFirstHtmlElementByName(page, name);
        assert(elem instanceof HtmlTextInput);
        return ((HtmlTextInput) elem).setValueAttribute(text);
    }

    /**
     * Verify that the text in the text box is as expected in the first
     * element with the given name.
     */
    public static void checkTextByName(HtmlPage page, String name, String text) {
        HtmlElement elem = getFirstHtmlElementByName(page, name);
        assert(elem instanceof HtmlTextInput);
        assert(text.equals(((HtmlTextInput) elem).getValueAttribute()));
    }

    /**
     * Find the radio buttons with a particular name and click the first one
     * with a particular value.  Throws an assert error if no such
     * radio button is found.
     */
    public static void clickRadio(HtmlPage page, String name, String value) 
        throws Exception {
        List<HtmlElement> radios = page.getElementsByName(name);
        HtmlInput radioToClick = null;
        for(HtmlElement radio: radios) {
            assert(radio instanceof HtmlInput);
            if (((HtmlInput)radio).getValueAttribute().equals(value)) {
                radioToClick = (HtmlInput) radio;
                break;
            }
        }
        assert(radioToClick != null);
        radioToClick.click();
    }

    /**
     * Check that the radio button with a particular value is selected.
     * Throws an assert if no such radio button is found.
     */
    public static void checkRadioSelected(HtmlPage page, String name, 
                                          String value) {
        List<HtmlElement> radios = page.getElementsByName(name);
        HtmlInput radioWithValue = null;
        for(HtmlElement radio: radios) {
            assert(radio instanceof HtmlInput);
            if (((HtmlInput)radio).getValueAttribute().equals(value)) {
                radioWithValue = (HtmlInput) radio;
                break;
            }
        }
        assert(radioWithValue != null);
        assert(radioWithValue.isChecked());
    }

    /**
     * Choose a selection value from a dropdown menu.
     */
    public static void chooseSelection(HtmlPage page, String name, 
                                       String value) {
        HtmlElement elem = Util.getFirstHtmlElementByName(page, name);
        assert(elem instanceof HtmlSelect);
        HtmlSelect select = (HtmlSelect) elem;
        HtmlOption match = select.getOptionByValue(value);
        assert(match != null);
        match.setSelected(true);
    }

    /**
     * Check if the selection value in a dropdown menu is chosen.
     */
    public static void checkSelection(HtmlPage page, String name, 
                                      String value) {
        HtmlElement elem = Util.getFirstHtmlElementByName(page, name);
        assert(elem instanceof HtmlSelect);
        HtmlSelect select = (HtmlSelect) elem;
        HtmlOption match = select.getOptionByValue(value);
        assert(match != null);
        assert(match.isSelected() == true);
    }

    /**
     * Check that this url cannot be reached by the user logged in
     * through this webclient.
     */
    public static void checkPageUnreachable(WebClient wc, String url) 
        throws Exception {
        boolean failure = false;
        try {
            HtmlPage page = wc.goTo(url);
        } catch (com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException 
                 fhsce) {
            failure = true;
        }
        assert (failure == true);
    }

    /**
     * Create a file in the workspace for possible upload, etc.
     */
    public static void createFileInWorkspace(AbstractProject job, 
                                             String filename, 
                                             final String fileContent) 
        throws Exception {
        FilePath workspace = job.getSomeWorkspace();
        if (!workspace.exists()) {
            workspace.mkdirs();
        }
        FilePath file = workspace.child(filename);
        file.act(new FileCallable<Void>() {
            @Override
            public Void invoke(File f, VirtualChannel channel) 
                throws IOException {
                FileWriter fw = new FileWriter(f);
                fw.write(fileContent);
                fw.close();
                return null;
            }
            @Override
            public void checkRoles(RoleChecker arg0) throws SecurityException {
                // TODO Auto-generated method stub
            }
        });
    }
}
