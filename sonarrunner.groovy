#!./lib/runner.groovy
// Generates server-side metadata for Sonar Runner
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlPage
import java.util.regex.Pattern
import net.sf.json.JSONObject
import hudson.util.VersionNumber

def listUp(url,pattern) {
    wc = new WebClient()
    wc.javaScriptEnabled = false;
    wc.cssEnabled = false;
    HtmlPage p = wc.getPage(url);
    pattern=Pattern.compile(pattern);

    return p.getAnchors().collect { HtmlAnchor a ->
        m = pattern.matcher(a.hrefAttribute)
        if(m.find()) {
            ver=m.group(1)
            url = p.getFullyQualifiedUrl(a.hrefAttribute + "sonar-runner-" + ver + ".zip");
            return ["id":ver, "name": "Sonar Runner " + ver, "url":url.toExternalForm()]
        }
        return null;
    }.findAll { it!=null }.sort { o1,o2 ->
        try {
            def v1 = new VersionNumber(o1.id)
            try {
                new VersionNumber(o2.id).compareTo(v1)
            } catch (IllegalArgumentException _2) {
                -1
            }
        } catch (IllegalArgumentException _1) {
            try {
                new VersionNumber(o2.id)
                1
            } catch (IllegalArgumentException _2) {
                o2.id.compareTo(o1.id)
            }
        }
    }
}

def store(key,o) {
    JSONObject envelope = JSONObject.fromObject(["list": o]);
    lib.DataWriter.write(key,envelope);
}

store("hudson.plugins.sonar.SonarRunnerInstaller",  listUp("http://repository.codehaus.org/org/codehaus/sonar-plugins/sonar-runner/",  "([0-9.]+)/\$"))

