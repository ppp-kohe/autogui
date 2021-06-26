package org.autogui.base.mapping;

import org.autogui.base.JsonWriter;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.swing.GuiSwingMapperSet;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * the class for manually setting preferences to target objects
 * <pre>
 *     GuiPreferencesLoader.get().apply(obj);
 *       //setting properties from default saved prefs ("/pack/to/Cls/$default")
 * </pre>
 *
 * <pre>
 *     GuiPreferencesLoader.get().withPrefsGetterByNameEquals("p").apply(obj);
 *       //setting properties from saved prefs as "p"
 *        //("/pack/to/Cls/$saved/$N" and "$name" is "p")
 * </pre>
 *
 * <pre>
 *     var restArgs = GuiPreferencesLoader.get().parseArgs(obj, Arrays.asList(args));
 *       //e.g. args={"-l", "p"} then, load and apply prefs "p" to obj
 * </pre>
 * @since 1.4
 */
public class GuiPreferencesLoader {
    protected GuiTypeBuilder typeBuilder;
    protected Function<GuiPreferences, GuiPreferences> prefsGetter = GuiPreferencesLoader::findSavedPrefsDefault;

    protected GuiMappingContext context;

    protected Map<PrefsLoaderCommandArg, List<String>> argNames = new HashMap<>();

    public static GuiPreferencesLoader get() {
        return new GuiPreferencesLoader();
    }

    public GuiPreferencesLoader withTypeBuilder(GuiTypeBuilder typeBuilder) {
        this.typeBuilder = typeBuilder;
        return this;
    }

    /**
     * set the type-builder to {@link org.autogui.base.type.GuiTypeBuilder.GuiTypeBuilderRelaxed}.
     *  This is required for taking prefs of the app launched by <code>AutoGuiShell.showLive(...).</code>
     * @return this
     */
    public GuiPreferencesLoader withTypeBuilderRelaxed() {
        return withTypeBuilder(new GuiTypeBuilder.GuiTypeBuilderRelaxed());
    }

    /**
     * sets the prefs selector that takes the root prefs and returns a saved preferences;
     *  typically, it takes an element of {@link GuiPreferences#getSavedStoreListAsRoot()}
     * @param prefsGetter the selector function
     * @return this
     */
    public GuiPreferencesLoader withPrefsGetter(Function<GuiPreferences, GuiPreferences> prefsGetter) {
        this.prefsGetter = prefsGetter;
        return this;
    }

    /**
     * {@link #withPrefsGetter(Function)} with {@link #findSavedPrefsByName(GuiPreferences, Predicate)}
     *   for selecting named prefs
     * @param name the name compared to the "$name" value
     * @return this
     */
    public GuiPreferencesLoader withPrefsGetterByNameEquals(String name) {
        return withPrefsGetter(e -> findSavedPrefsByName(e, name::equals));
    }

    /**
     * {@link #withPrefsGetter(Function)} with {@link #findSavedPrefsDefault(GuiPreferences)}
     *  for selecting default prefs
     * @return this
     */
    public GuiPreferencesLoader withPrefsGetterDefault() {
        return withPrefsGetter(GuiPreferencesLoader::findSavedPrefsDefault);
    }

    /**
     * creates a {@link GuiMappingContext} for the given object,
     *  with {@link GuiTypeBuilder} previously set by {@link #withTypeBuilder(GuiTypeBuilder)}
     * @param obj the target object of the creating context
     * @return this
     */
    public GuiPreferencesLoader withContextForObject(Object obj) {
        if (typeBuilder == null) {
            typeBuilder = new GuiTypeBuilder();
        }
        GuiMappingContext context = new GuiMappingContext(typeBuilder.get(obj.getClass()), obj);
        GuiSwingMapperSet.getReprDefaultSet().matchAndSetNotifiersAsInit(context);
        return withContext(context);
    }

    public GuiPreferencesLoader withContext(GuiMappingContext context) {
        this.context = context;
        return this;
    }

    public List<GuiPreferences> getSavedPrefs() {
        return getSavedPrefs(context);
    }

    public List<GuiPreferences> getSavedPrefsWithDefault() {
        return getSavedPrefsWithDefault(context);
    }

    public List<String> getSavedPrefsNames() {
        return getSavedPrefs(context).stream()
                .map(GuiPreferencesLoader::getPrefsName)
                .collect(Collectors.toList());
    }

    public static List<GuiPreferences> getSavedPrefs(GuiMappingContext context) {
        return context.getPreferences().getSavedStoreListAsRoot();
    }

    public static List<GuiPreferences> getSavedPrefsWithDefault(GuiMappingContext context) {
        List<GuiPreferences> all = new ArrayList<>();
        GuiPreferences rootPrefs = context.getPreferences();
        GuiPreferences p = findSavedPrefsDefault(rootPrefs);
        if (p != null) {
            all.add(p);
        }
        all.addAll(getSavedPrefs(context));
        return all;
    }

    /**
     * loading specified saved prefs to the obj
     *   The prefs can be selected by {@link #withPrefsGetterDefault()} (default)
     *   or {@link #withPrefsGetterByNameEquals(String)}
     * @param obj the context target supplied by {@link #withContext(GuiMappingContext)}
     * @return {@link #apply()}
     */
    public Object apply(Object obj) {
        withContextForObject(obj);
        return apply();
    }

    /**
     * applies the current context to {@link #applyForContext(GuiMappingContext)}
     * @return the returned object by {@link #applyForContext(GuiMappingContext)}
     */
    public Object apply() {
        return applyForContext(context);
    }

    /**
     * takes prefs from the context and selects a prefs by the getter set by {@link #withPrefsGetter(Function)},
     *   and calls {@link #load(GuiPreferences)} with the prefs
     * @param context the context for prefs
     * @return the returned object by load
     */
    public Object applyForContext(GuiMappingContext context) {
        GuiPreferences prefs = takeSavedPrefs(context);
        return load(prefs);
    }

    public GuiPreferences takeSavedPrefs() {
        return takeSavedPrefs(context);
    }

    public GuiPreferences takeSavedPrefs(GuiMappingContext context) {
        return prefsGetter.apply(context.getPreferences());
    }

    public static GuiPreferences findSavedPrefsByName(GuiPreferences rootPrefs, Predicate<String> name) {
        for (GuiPreferences prefs : rootPrefs.getSavedStoreListAsRoot()) {
            String prefsName = getPrefsName(prefs);
            if (name.test(prefsName)) {
                return prefs;
            }
        }
        return null;
    }

    public static String getPrefsName(GuiPreferences savedPrefs) {
        return savedPrefs.getValueStore().getString("$name", "");
    }

    public static GuiPreferences findSavedPrefsDefault(GuiPreferences rootPrefs) {
        GuiPreferences.GuiValueStore store = rootPrefs.getValueStoreRootFromRepresentation();
        if (store == null) {
            return null;
        } else {
            return store.getPreferences();
        }
    }

    /**
     * {@link #load(GuiPreferences, GuiReprValue.ObjectSpecifier)} with {@link GuiReprValue#NONE}
     * @param prefs the source prefs
     * @return the current source value of the context
     */
    public Object load(GuiPreferences prefs) {
        return load(prefs, GuiReprValue.NONE);
    }

    /**
     * sets current saved value taken from the prefs to the properties of the source object of the context.
     * It takes the context from prefs {@link GuiPreferences#getContext()}
     * @param prefs the source prefs
     * @param specifier the specifier
     * @return context's {@link GuiMappingContext#getSource()} value or null
     */
    public Object load(GuiPreferences prefs, GuiReprValue.ObjectSpecifier specifier) {
        GuiMappingContext context = prefs.getContext();
        if (context.getRepresentation() instanceof GuiReprObjectPane) {
            context.getChildren().stream()
                    .map(prefs::getChild)
                    .filter(Objects::nonNull)
                    .forEach(subPrefs -> load(subPrefs, specifier.child(false)));
            if (context.getSource() != null) {
                return context.getSource().getValue();
            } else {
                return null;
            }
        } else if (context.isReprValue()) {
            Object value = prefs.getCurrentValue();
            if (value != null) {
                try {
                    return ((GuiReprValue) context.getRepresentation())
                            .updateWithParentSource(context, value, specifier.child(false));
                } catch (Throwable error) {
                    handleError(error, prefs);
                    return null;
                }
            } else {
                return null; //???
            }
        } else {
            if (context.getSource() != null) {
                return context.getSource().getValue();
            } else {
                return null;
            }
        }
    }

    public void handleError(Throwable error, GuiPreferences prefs) {
        throw new PrefsLoadException(error, prefs);
    }

    public static class PrefsLoadException extends RuntimeException {
        protected GuiPreferences prefs;

        public PrefsLoadException(Throwable cause, GuiPreferences prefs) {
            super(cause);
            this.prefs = prefs;
        }

        public GuiPreferences getPrefs() {
            return prefs;
        }
    }

    /////////////


    public enum PrefsLoaderCommandArg {
        /** <code>-l|--prefs-load</code>*/
        LoadByName {
            @Override
            List<String> defaultValues() {
                return Arrays.asList("-l", "--prefs-load");
            }

            @Override
            String description() {
                return " <name> : load specified saved prefs (\".\" for default)";
            }
        },
        /** <code>-s|--prefs-show-names</code>*/
        ShowNames {
            @Override
            List<String> defaultValues() {
                return Arrays.asList("-s", "--prefs-show-names");
            }

            @Override
            String description() {
                return " : list names of saved prefs";
            }
        },
        /** <code>-j|--prefs-json</code>*/
        ShowJsonByName {
            @Override
            List<String> defaultValues() {
                return Arrays.asList("-j", "--prefs-json");
            }

            @Override
            String description() {
                return "<name> : search named prefs and show JSON";
            }
        },
        /** <code>-h|--help|-help</code>*/
        Help {
            @Override
            List<String> defaultValues() {
                return Arrays.asList("-h", "--help", "-help");
            }

            @Override
            String description() {
                return ": show the help";
            }
        };

        abstract List<String> defaultValues();
        abstract String description();
    }

    /**
     * sets command line args for {@link #parseArgs(List)}.
     * e.g.
     *  <pre>
     * .withArgNames(Map.of(
     *     LoadByName, List.of("-L", "--load"), //replace existing command options
     *     Help, List.of())) //no values: disable the option
     *   //for unspecified keys, default args are used.
     *  </pre>
     * @param argNames the maps for customizing args
     * @return this
     */
    public GuiPreferencesLoader withArgNames(Map<PrefsLoaderCommandArg, List<String>> argNames) {
        this.argNames = argNames;
        return this;
    }

    /**
     * @param obj the target obj
     * @param args command args
     * @return rest of consumed args
     * @see #parseArgs(Object, List)
     */
    public List<String> parseArgs(Object obj, String... args) {
        return parseArgs(obj, Arrays.asList(args));
    }

    /**
     *
     * @param obj the target obj for calling {@link #withContext(GuiMappingContext)}
     * @param args command args
     * @return rest of consumed args
     * @see #parseArgs(List)
     */
    public List<String> parseArgs(Object obj, List<String> args) {
        withContextForObject(obj);
        return parseArgs(args);
    }

    /**
     * consume head args for {@link PrefsLoaderCommandArg} and do the option.
     *   <ul>
     *       <li>{@link PrefsLoaderCommandArg#LoadByName} :
     *             <code>-l &lt;name&gt;</code>. consume 2 elements and {@link #runLoad(String)} </li>
     *       <li>{@link PrefsLoaderCommandArg#ShowNames} :
     *              <code>-s</code>. consume 1 element and {@link #runShowNames()}</li>
     *       <li>{@link PrefsLoaderCommandArg#ShowJsonByName} :
     *             <code>-j  &lt;name&gt;</code>. consume 2 elements and {@link #runJson(String)}</li>
     *        <li>{@link PrefsLoaderCommandArg#Help} :
     *             <code>-h</code>. checks the top element but not consume the -h, and {@link #runHelp()}</li>
     *   </ul>
     *   If the top args do not match with those commands, nothing will happen.
     *   The method supposes the context is already set by {@link #withContext(GuiMappingContext)}
     * @param args the consumed args
     * @return the rest of consumed args
     */
    public List<String> parseArgs(List<String> args) {
        if (args.isEmpty()) {
            return args;
        }

        List<String> loadByName = argNames.getOrDefault(PrefsLoaderCommandArg.LoadByName, PrefsLoaderCommandArg.LoadByName.defaultValues());
        List<String> showNames = argNames.getOrDefault(PrefsLoaderCommandArg.ShowNames, PrefsLoaderCommandArg.ShowNames.defaultValues());
        List<String> jsonByName = argNames.getOrDefault(PrefsLoaderCommandArg.ShowJsonByName, PrefsLoaderCommandArg.ShowJsonByName.defaultValues());
        List<String> help = argNames.getOrDefault(PrefsLoaderCommandArg.Help, PrefsLoaderCommandArg.Help.defaultValues());
        int i = 0;
        String arg = args.get(i);
        if (loadByName.contains(arg)) {
            ++i;
            arg = args.get(i);
            runLoad(arg);
            ++i;
        } else if (showNames.contains(arg)) {
            ++i;
            runShowNames();
        } else if (jsonByName.contains(arg)) {
            ++i;
            arg = args.get(i);
            runJson(arg);
            ++i;
        } else if (help.contains(arg)) {
            runHelp();
            i= 0;
        }
        return args.subList(i, args.size());
    }

    /**
     * set prefs getter with arg and {@link #apply()}
     * @param arg the name for {@link #withPrefsGetterByNameEquals(String)}
     *             or "." for {@link #withPrefsGetterDefault()}
     */
    public void runLoad(String arg) {
        if (arg.equals(".")) {
            withPrefsGetterDefault();
        } else {
            withPrefsGetterByNameEquals(arg);
        }
        apply();
    }

    /**
     * {@link #println(Object)} names of saved prefs for current context
     */
    public void runShowNames() {
        getSavedPrefsNames()
                .forEach(this::println);
    }

    /**
     * get the prefs and {@link #println(Object)} JSON source of the prefs
     * @param arg the name for {@link #withPrefsGetterByNameEquals(String)}
     *             or "." for {@link #withPrefsGetterDefault()}
     */
    public void runJson(String arg) {
        if (arg.equals(".")) {
            withPrefsGetterDefault();
        } else {
            withPrefsGetterByNameEquals(arg);
        }
        GuiPreferences prefs = takeSavedPrefs();
        Object json = prefs.toJson();
        println(JsonWriter.create()
                .withNewLines(true)
                .write(json)
                .toSource());
    }

    /**
     * {@link #println(Object)} help messages of {@link PrefsLoaderCommandArg}
     */
    public void runHelp() {
        for (PrefsLoaderCommandArg arg : PrefsLoaderCommandArg.values()) {
            List<String> vs = argNames.getOrDefault(arg, arg.defaultValues());
            if (!vs.isEmpty()) { //empty: disabled
                println(String.join("|", vs) + " " + arg.description());
            }
        }
    }

    public void println(Object o) {
        System.out.println(o);
    }
}
