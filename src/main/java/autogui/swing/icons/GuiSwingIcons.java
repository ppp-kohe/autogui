package autogui.swing.icons;

import autogui.swing.GuiSwingViewLabel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** an {@link Icon} factory*/
public class GuiSwingIcons {
    protected static GuiSwingIcons instance = new GuiSwingIcons();
    protected Map<String, Icon> iconMap = new HashMap<>();
    //to avoid concurrent modification to iconMap, define another map
    protected Map<String, Icon> pressedIconMap = new HashMap<>();
    protected List<String> iconWords = new ArrayList<>();
    protected Map<String, String> synonyms = new HashMap<>();

    protected Icon defaultIcon;
    protected String suffix = "@2x.png";

    public static GuiSwingIcons getInstance() {
        return instance;
    }

    public GuiSwingIcons() {
        initSynonyms();
    }

    public Icon getIcon(String name) {
        return iconMap.computeIfAbsent("action-" + name, k -> loadIconOrDefault(name));
    }

    public Icon getIcon(String prefix, String name, int width, int height) {
        return iconMap.computeIfAbsent(prefix + name, k -> loadIcon(prefix, name, suffix, width, height));
    }

    public Icon loadIconOrDefault(String name) {
        Icon icon = loadIcon(name);
        if (icon == null) {
            icon = getDefaultIcon(name);
        }
        return icon;
    }

    public Icon loadIcon(String name) {
        return loadIcon("action-",
                synonyms.getOrDefault(name, name), suffix, 32, 32);
    }

    public Icon loadIcon(String prefix, String name, String suffix, int width, int height) {
        try {
            URL url = getClass().getResource(prefix + name + suffix);
            if (url == null) {
                return null;
            } else {
                return new ResourceIcon(
                        ImageIO.read(url), width, height);
            }
        } catch (Exception ex) {
            return null;
        }
    }

    public Icon getDefaultIcon(String name) {
        if (defaultIcon == null) {
            URL url = getClass().getResource("action" + suffix);
            if (url != null) {
                try {
                    defaultIcon = new ResourceIcon(ImageIO.read(url), 32, 32);
                } catch (Exception ex) {
                    //
                }
            }
            if (defaultIcon == null) {
                int w = 64;
                int h = 64;
                BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
                Graphics2D g = image.createGraphics();
                {
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    g.setColor(new Color(0, 0, 0, 0));
                    g.fillRect(0, 0, w, h);

                    g.setColor(new Color(245, 245, 245));
                    RoundRectangle2D.Float rr = new RoundRectangle2D.Float(5, h / 2 - 10, w - 10, 20, 5, 5);
                    g.fill(rr);

                    g.setStroke(new BasicStroke(0.7f));
                    g.setColor(new Color(160, 160, 160));
                    g.draw(rr);
                }
                g.dispose();
                defaultIcon = new ResourceIcon(image, 32, 32);
            }
        }
        return defaultIcon;
    }

    public void addSynonym(String name, String... synonyms) {
        if (!iconWords.contains(name)) {
            iconWords.add(name);
        }
        for (String synonym : synonyms) {
            this.synonyms.put(synonym, name);
        }
    }

    public void addIconWords(String... names) {
        for (String name : names) {
            if (!iconWords.contains(name)) {
                iconWords.add(name);
            }
        }
    }

    public List<String> getIconWords() {
        return iconWords;
    }

    public Map<String, Icon> getIconMap() {
        return iconMap;
    }

    public Map<String, Icon> getPressedIconMap() {
        return pressedIconMap;
    }

    public Map<String, String> getSynonyms() {
        return synonyms;
    }

    /**
     * returns a gray version of the icon obtained by getIcon(name).
     * this can be set for the pressed icon of an button:
     * <pre>
     *     button.setPressedIcon(icons.getPressedIcon(name));
     * </pre>
     * Default impl. of some UI automatically generates the pressed icon for an ImageIcon,
     *    but does not for another icon type.
     * @param name the action icon name
     * @return the pressed icon for the name
     */
    public Icon getPressedIcon(String name) {
        return pressedIconMap.computeIfAbsent("action-" + name , k -> loadPressedIcon(name));
    }

    public Icon loadPressedIcon(String name) {
        return loadPressedIcon(getIcon(name));
    }

    public Icon loadPressedIcon(Icon icon) {
        if (icon instanceof ResourceIcon) {
            return ((ResourceIcon) icon).getPressedIcon();
        } else if (icon instanceof ImageIcon){
            return new ImageIcon(GrayFilter.createDisabledImage(((ImageIcon) icon).getImage()));
        }
        return icon;
    }

    public Icon getPressedIcon(String prefix, String name, int width, int height) {
        return pressedIconMap.computeIfAbsent(prefix + name, k -> loadPressedIcon(loadIcon(prefix, name, suffix, width, height)));
    }

    /** an {@link Icon} with an {@link Image} and size*/
    public static class ResourceIcon implements Icon {
        protected Image image;
        protected float width;
        protected float height;

        public ResourceIcon(Image image, float width, float height) {
            this.image = image;
            this.width = width;
            this.height = height;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g;
            g2.drawImage(image, x, y, (int) width, (int) height, c);
        }

        @Override
        public int getIconWidth() {
            return (int) width;
        }

        @Override
        public int getIconHeight() {
            return (int) height;
        }

        public Image getImage() {
            return image;
        }

        public ResourceIcon getPressedIcon() {
            return new ResourceIcon(GrayFilter.createDisabledImage(image), width, height);
        }
    }

    public static String PRESSED_ICON_KEY = "autoguiPressedIcon";

    /** an action button with the customized border */
    public static class ActionButton extends JButton {
        public ActionButton(Action a) {
            super(a);
            setFocusable(true);
            //setBorderPainted(false);
            setContentAreaFilled(false);
            setBorder(BorderFactory.createCompoundBorder(
                    new GuiSwingViewLabel.FocusBorder(this),
                    BorderFactory.createEmptyBorder(2, 5, 2, 5)));
            setFocusPainted(false);

            Object o = a.getValue(PRESSED_ICON_KEY);
            if (o != null) {
                setPressedIcon((Icon) o);
            }
        }
    }


    /////////////////////////////

    public Icon getAcceptIcon() { return getIcon("accept"); }
    public Icon getAddIcon() { return getIcon("add"); }
    public Icon getAdjustIcon() { return getIcon("adjust"); }
    public Icon getApplyIcon() { return getIcon("apply"); }
    public Icon getBindIcon() { return getIcon("bind"); }
    public Icon getBuildIcon() { return getIcon("build"); }
    public Icon getCalculateIcon() { return getIcon("calculate"); }
    public Icon getCallIcon() { return getIcon("call"); }
    public Icon getChangeIcon() { return getIcon("change"); }
    public Icon getCheckIcon() { return getIcon("check"); }
    public Icon getClearIcon() { return getIcon("clear"); }
    public Icon getCloseIcon() { return getIcon("close"); }
    public Icon getCollectIcon() { return getIcon("collect"); }
    public Icon getCommitIcon() { return getIcon("commit"); }
    public Icon getCompareIcon() { return getIcon("compare"); }
    public Icon getCompleteIcon() { return getIcon("complete"); }
    public Icon getComposeIcon() { return getIcon("compose"); }
    public Icon getConfigureIcon() { return getIcon("configure"); }
    public Icon getConnectIcon() { return getIcon("connect"); }
    public Icon getConvertIcon() { return getIcon("convert"); }
    public Icon getCopyIcon() { return getIcon("copy"); }
    public Icon getCountIcon() { return getIcon("count"); }
    public Icon getCreateIcon() { return getIcon("create"); }
    public Icon getDeactivateIcon() { return getIcon("deactivate"); }
    public Icon getDecodeIcon() { return getIcon("decode"); }
    public Icon getDefineIcon() { return getIcon("define"); }
    public Icon getDeleteIcon() { return getIcon("delete"); }
    public Icon getDeriveIcon() { return getIcon("derive"); }
    public Icon getDivideIcon() { return getIcon("divide"); }
    public Icon getDownIcon() { return getIcon("down"); }
    public Icon getDrawIcon() { return getIcon("draw"); }
    public Icon getEncodeIcon() { return getIcon("encode"); }
    public Icon getEnsureIcon() { return getIcon("ensure"); }
    public Icon getExportIcon() { return getIcon("export"); }
    public Icon getExtractIcon() { return getIcon("extract"); }
    public Icon getFillIcon() { return getIcon("fill"); }
    public Icon getFindIcon() { return getIcon("find"); }
    public Icon getFocusIcon() { return getIcon("focus"); }
    public Icon getGetIcon() { return getIcon("get"); }
    public Icon getHandleIcon() { return getIcon("handle"); }
    public Icon getHelpIcon() { return getIcon("help"); }
    public Icon getHideIcon() { return getIcon("hide"); }
    public Icon getListIcon() { return getIcon("list"); }
    public Icon getLoadIcon() { return getIcon("load"); }
    public Icon getLockIcon() { return getIcon("lock"); }
    public Icon getMapIcon() { return getIcon("map"); }
    public Icon getMarkIcon() { return getIcon("mark"); }
    public Icon getMergeIcon() { return getIcon("merge"); }
    public Icon getMinusIcon() { return getIcon("minus"); }
    public Icon getMoveIcon() { return getIcon("move"); }
    public Icon getMultiplyIcon() { return getIcon("multiply"); }
    public Icon getNegateIcon() { return getIcon("negate"); }
    public Icon getNextIcon() { return getIcon("next"); }
    public Icon getNormalizeIcon() { return getIcon("normalize"); }
    public Icon getNotifyIcon() { return getIcon("notify"); }
    public Icon getPaintIcon() { return getIcon("paint"); }
    public Icon getPeekIcon() { return getIcon("peek"); }
    public Icon getPlusIcon() { return getIcon("plus"); }
    public Icon getPreviousIcon() { return getIcon("previous"); }
    public Icon getPrintIcon() { return getIcon("print"); }
    public Icon getProvideIcon() { return getIcon("provide"); }
    public Icon getPublishIcon() { return getIcon("publish"); }
    public Icon getPutIcon() { return getIcon("put"); }
    public Icon getReadIcon() { return getIcon("read"); }
    public Icon getReceiveIcon() { return getIcon("receive"); }
    public Icon getReduceIcon() { return getIcon("reduce"); }
    public Icon getRegisterIcon() { return getIcon("register"); }
    public Icon getReleaseIcon() { return getIcon("release"); }
    public Icon getRemoveIcon() { return getIcon("remove"); }
    public Icon getRenameIcon() { return getIcon("rename"); }
    public Icon getReplaceIcon() { return getIcon("replace"); }
    public Icon getRequestIcon() { return getIcon("request"); }
    public Icon getResizeIcon() { return getIcon("resize"); }
    public Icon getResolveIcon() { return getIcon("resolve"); }
    public Icon getRetainIcon() { return getIcon("retain"); }
    public Icon getReverseIcon() { return getIcon("reverse"); }
    public Icon getRotateIcon() { return getIcon("rotate"); }
    public Icon getSaveIcon() { return getIcon("save"); }
    public Icon getScheduleIcon() { return getIcon("schedule"); }
    public Icon getScrollIcon() { return getIcon("scroll"); }
    public Icon getSelectIcon() { return getIcon("select"); }
    public Icon getSetIcon() { return getIcon("set"); }
    public Icon getShiftIcon() { return getIcon("shift"); }
    public Icon getShowIcon() { return getIcon("show"); }
    public Icon getSkipIcon() { return getIcon("skip"); }
    public Icon getSliceIcon() { return getIcon("slice"); }
    public Icon getSortIcon() { return getIcon("sort"); }
    public Icon getSplitIcon() { return getIcon("split"); }
    public Icon getStartIcon() { return getIcon("start"); }
    public Icon getStopIcon() { return getIcon("stop"); }
    public Icon getSyncIcon() { return getIcon("sync"); }
    public Icon getTrimIcon() { return getIcon("trim"); }
    public Icon getTryIcon() { return getIcon("try"); }
    public Icon getUndoIcon() { return getIcon("undo"); }
    public Icon getUnlockIcon() { return getIcon("unlock"); }
    public Icon getUnregisterIcon() { return getIcon("unregister"); }
    public Icon getUnwrapIcon() { return getIcon("unwrap"); }
    public Icon getUpIcon() { return getIcon("up"); }
    public Icon getUpdateIcon() { return getIcon("update"); }
    public Icon getUseIcon() { return getIcon("use"); }
    public Icon getWaitIcon() { return getIcon("wait"); }
    public Icon getWrapIcon() { return getIcon("wrap"); }
    public Icon getWriteIcon() { return getIcon("write"); }

    public void initSynonyms() {
        addIconWords(
                "accept","add","adjust","apply","bind","build","calculate","call","change","check","clear"
                ,"close","collect","commit","compare","complete","compose","configure","connect","convert","copy","count"
                ,"create","deactivate","decode","define","delete","derive","divide","down","draw","encode","ensure"
                ,"export","extract","fill","find","focus","get","handle","help","hide","list","load"
                ,"lock","map","mark","merge","minus","move","multiply","negate","next","normalize","notify"
                ,"paint","peek","plus","previous","print","provide","publish","put","read","receive","reduce"
                ,"register","release","remove","rename","replace","request","resize","resolve","retain","reverse"
                ,"rotate","save","schedule","scroll","select","set","shift","show","skip","slice"
                ,"sort","split","start","stop","sync","trim","try","undo","unlock","unregister"
                ,"unwrap","up","update","use","wait","wrap","write");
        addSynonym("add", "insert", "append", "install", "join");
        addSynonym("apply", "compute");
        addSynonym("bind", "rebind");
        addSynonym("calculate", "evaluate", "eval");
        addSynonym("change", "modify");
        addSynonym("check", "validate", "revalidate", "test", "verify");
        addSynonym("clear", "reset", "empty", "flush", "format", "free", "init", "initialize", "refresh");
        addSynonym("compose", "group");
        addSynonym("convert", "to", "transform", "translate");
        addSynonym("copy", "clone", "duplicate");
        addSynonym("create", "new", "allocate", "generate", "make");
        addSynonym("deactivate", "unset", "deregister", "disable");
        addSynonym("find", "query", "filter", "lookup", "match", "matches", "search");
        addSynonym("get", "take", "acquire");
        addSynonym("load", "open");
        addSynonym("minus", "decrement", "subtract");
        addSynonym("paint", "repaint");
        addSynonym("plus", "sum", "accumulate", "increment");
        addSynonym("previous", "back");
        addSynonym("provide", "offer");
        addSynonym("put", "send", "dispatch", "post", "push", "redirect", "store", "submit", "transfer");
        addSynonym("remove", "uninstall", "deinstall", "dispose", "pop");
        addSynonym("resolve", "solve");
        addSynonym("reverse", "rewind", "rollback");
        addSynonym("save", "report");
        addSynonym("set", "enable", "activate");
        addSynonym("start", "fire", "begin", "exec", "execute", "invoke", "process", "run", "starts", "play");
        addSynonym("stop", "pause", "abort", "cancel", "destroy", "end", "ends", "finish", "invalidate", "shutdown");
        addSynonym("trim", "truncate");
        addSynonym("unregister", "unbind");
    }


}
