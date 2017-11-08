package autogui.swing.icons;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiSwingIcons {
    protected static GuiSwingIcons instance = new GuiSwingIcons();

    protected Map<String, Icon> iconMap = new HashMap<>();
    protected List<String> iconWords = new ArrayList<>();
    protected Map<String, String> synonyms = new HashMap<>();

    protected Icon defaultIcon;

    public static GuiSwingIcons getInstance() {
        return instance;
    }

    public GuiSwingIcons() {
        initSynonyms();
    }

    public Icon getIcon(String name) {
        return iconMap.computeIfAbsent(name, this::loadIconOrDefault);
    }

    public Icon loadIconOrDefault(String name) {
        Icon icon = loadIcon(name);
        if (icon == null) {
            icon = getDefaultIcon(name);
        }
        return icon;
    }

    public Icon loadIcon(String name) {
        try {
            String iconName = synonyms.getOrDefault(name, name);

            URL url = getClass().getResource("autogui-icon-" + iconName + "@2x.png");
            if (url == null) {
                return null;
            } else {
                return new ResourceIcon(
                        ImageIO.read(url), 32, 32);
            }
        } catch (Exception ex) {
            return null;
        }
    }

    public Icon getDefaultIcon(String name) {
        URL url = getClass().getResource("autogui-icon@2x.png");
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

    public Map<String, String> getSynonyms() {
        return synonyms;
    }

    public static class ResourceIcon implements Icon {
        protected BufferedImage image;
        protected int width;
        protected int height;
        protected AffineTransformOp op;

        public ResourceIcon(BufferedImage image, int width, int height) {
            this.image = image;
            this.width = width;
            this.height = height;
            op = new AffineTransformOp(getTransform(), getRenderingHings());
        }

        protected AffineTransform getTransform() {
            float imageWidth = image.getWidth();
            float imageHeight = image.getHeight();
            return new AffineTransform(
                    width / imageWidth, 0, 0,
                    height / imageHeight, 0, 0);
        }


        protected RenderingHints getRenderingHings() {
            RenderingHints hints = new RenderingHints(new HashMap<>());
            hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            hints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            return hints;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g;
            g2.drawImage(image, op, 0, 0);
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
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
    public Icon getCreateIcon() { return getIcon("create"); }
    public Icon getDeactivateIcon() { return getIcon("deactivate"); }
    public Icon getDecodeIcon() { return getIcon("decode"); }
    public Icon getDefineIcon() { return getIcon("define"); }
    public Icon getDeleteIcon() { return getIcon("delete"); }
    public Icon getDeriveIcon() { return getIcon("derive"); }
    public Icon getDivideIcon() { return getIcon("divide"); }
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
    public Icon getNormalizeIcon() { return getIcon("normalize"); }
    public Icon getNotifyIcon() { return getIcon("notify"); }
    public Icon getPaintIcon() { return getIcon("paint"); }
    public Icon getPeekIcon() { return getIcon("peek"); }
    public Icon getPlusIcon() { return getIcon("plus"); }
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
    public Icon getUnsetIcon() { return getIcon("unset"); }
    public Icon getUnwrapIcon() { return getIcon("unwrap"); }
    public Icon getUpdateIcon() { return getIcon("update"); }
    public Icon getUseIcon() { return getIcon("use"); }
    public Icon getWaitIcon() { return getIcon("wait"); }
    public Icon getWrapIcon() { return getIcon("wrap"); }
    public Icon getWriteIcon() { return getIcon("write"); }

    public void initSynonyms() {
        addIconWords(
                "accept","add","adjust","apply","bind","build","calculate","change","check","clear"
                ,"close","collect","commit","compare","complete","compose","configure","connect","convert","copy"
                ,"create","deactivate","decode","define","delete","derive","divide","draw","encode","ensure"
                ,"export","extract","fill","find","focus","get","handle","hide","list","load"
                ,"lock","map","mark","merge","minus","move","multiply","negate","normalize","notify"
                ,"paint","peek","plus","print","provide","publish","put","read","receive","reduce"
                ,"register","release","remove","rename","replace","request","resize","resolve","retain","reverse"
                ,"rotate","save","schedule","scroll","select","set","shift","show","skip","slice"
                ,"sort","split","start","stop","sync","trim","try","undo","unlock","unregister"
                ,"unset","unwrap","update","use","wait","wrap","write");
        addSynonym("add", "insert", "install", "append", "join");
        addSynonym("apply", "compute");
        addSynonym("bind", "rebind");
        addSynonym("calculate", "evaluate", "eval");
        addSynonym("change", "modify");
        addSynonym("check", "validate", "verify", "revalidate", "test");
        addSynonym("clear", "reset", "init", "format", "empty", "initialize", "refresh", "flush", "free");
        addSynonym("compose", "group");
        addSynonym("convert", "to", "transform", "translate");
        addSynonym("copy", "clone", "duplicate");
        addSynonym("create", "new", "make", "allocate", "generate");
        addSynonym("deactivate", "deregister", "disable");
        addSynonym("find", "query", "filter", "search", "match", "matches", "lookup");
        addSynonym("get", "take", "acquire");
        addSynonym("load", "open");
        addSynonym("minus", "subtract", "decrement");
        addSynonym("paint", "repaint");
        addSynonym("plus", "sum", "accumulate", "increment");
        addSynonym("provide", "offer");
        addSynonym("put", "send", "post", "submit", "dispatch", "push", "redirect", "store", "transfer");
        addSynonym("remove", "uninstall", "dispose", "pop", "deinstall");
        addSynonym("reverse", "rewind", "rollback");
        addSynonym("set", "enable", "activate");
        addSynonym("start", "fire", "process", "invoke", "run", "execute", "exec", "starts", "begin");
        addSynonym("stop", "pause", "destroy", "invalidate", "cancel", "shutdown", "abort", "ends", "finish", "end");
        addSynonym("trim", "truncate");
        addSynonym("unregister", "unbind");
    }


}
