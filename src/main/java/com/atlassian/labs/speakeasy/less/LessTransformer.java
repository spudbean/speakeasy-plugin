package com.atlassian.labs.speakeasy.less;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.webresource.transformer.AbstractStringTransformedDownloadableResource;
import com.atlassian.plugin.webresource.transformer.AbstractTransformedDownloadableResource;
import com.atlassian.plugin.webresource.transformer.WebResourceTransformer;
import org.apache.commons.io.IOUtils;
import org.dom4j.Element;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.0.41 -> https://github.com/cloudhead/less.js/tree/b7fb09fed7f269510b4a6be6f6075683007f1338
 */
public class LessTransformer implements WebResourceTransformer
{
    private static final Logger log = LoggerFactory.getLogger(LessTransformer.class);

    private final PluginAccessor pluginAccessor;

    public LessTransformer(PluginAccessor pluginAccessor)
    {
        this.pluginAccessor = pluginAccessor;
    }

    public DownloadableResource transform(Element element, ResourceLocation resourceLocation, String extrapath, DownloadableResource downloadableResource)
    {
        final String selfModule = element.attributeValue("selfModule");
        final Plugin plugin = pluginAccessor.getPlugin(selfModule);
        return new LessResource(plugin, resourceLocation, downloadableResource);
    }

    private class LessResource extends AbstractStringTransformedDownloadableResource
    {
        private final Plugin plugin;
        private final ResourceLocation originalLocation;

        private LessResource(final Plugin plugin, final ResourceLocation originalLocation, final DownloadableResource originalResource)
        {
            super(originalResource);
            this.plugin = plugin;
            this.originalLocation = originalLocation;
        }

        @Override
        public String getContentType()
        {
            return "text/css";
        }

        @Override
        protected String transform(final String originalContent)
        {
            try
            {
                return lessify(originalContent, false);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }


        private String lessify(String input, boolean compress) throws IOException
        {
            //TODO work out in future how to re-use parsed js (context) across requests
            // maybe move globalLoader into a per-call scope?
            // see https://developer.mozilla.org/En/Rhino_documentation/Scopes_and_Contexts

            final ContextFactory cf = new ContextFactory();
            final Context cx = cf.enter();
            try
            {
                final ScriptableObject topScope = cx.initStandardObjects();
                try
                {
                    cx.setOptimizationLevel(9);
                    loadjs(topScope, cx, "setup-env.js");
                    loadjs(topScope, cx, "less-concat.js");

                    final Function runLessRun = (Function) topScope.get("runLessRun", topScope);
                    final Object[] args = { new Loader(), input, compress };
                    final Object result = runLessRun.call(cx, topScope, topScope, args);

                    return Context.toString(result);
                }
                catch (JavaScriptException e)
                {
                    throw new RuntimeException(debugJsObject(cx, topScope, e.getValue()), e);
                }
            }
            finally
            {
                Context.exit();
            }
        }

        private String debugJsObject(Context cx, ScriptableObject scope, Object value)
        {
            if (value instanceof Scriptable) {
                Scriptable obj = (Scriptable)value;
                if (ScriptableObject.hasProperty(obj, "toSource")) {
                    Object v = ScriptableObject.getProperty(obj, "toSource");
                    if (v instanceof Function) {
                        Function f = (Function)v;
                        return String.valueOf(f.call(cx, scope, obj, new Object[0]));
                    }
                }
            }
            return String.valueOf(value);
        }


        private void loadjs(ScriptableObject topScope, Context cx, String name) throws IOException
        {
            log.debug("Loading JS {}", name);
            final InputStream in = getClass().getResourceAsStream(name);
            if (in == null)
            {
                throw new FileNotFoundException("Could not find JS resource " + name);
            }

            InputStreamReader reader = new InputStreamReader(in, "UTF-8");
            cx.evaluateReader(topScope, reader, name, 1, null);
        }

        public class Loader
        {
            public String load(String url) throws URISyntaxException, IOException
            {
                log.debug("Loading subresource {}", url);
                final URI destUri = new URI(originalLocation.getLocation()).resolve(url);
                final String destPath = destUri.getPath();
                final InputStream in = plugin.getResourceAsStream(destPath);
                final String result = IOUtils.toString(in);
                return result;
            }
        }
    }

}
