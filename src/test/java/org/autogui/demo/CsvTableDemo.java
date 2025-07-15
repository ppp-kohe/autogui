package org.autogui.demo;

import org.autogui.GuiIncluded;
import org.autogui.GuiInits;
import org.autogui.base.annotation.*;
import org.autogui.swing.AutoGuiShell;

import javax.swing.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@GuiIncluded
@GuiInits(window = @GuiInitWindow(width = 500, height = 800))
public class CsvTableDemo {

    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new CsvTableDemo());
    }

    String source = "https://www.data.jma.go.jp/stats/data/mdrr/tem_rct/alltable/mxtemsadext00_rct.csv";

    @GuiIncluded
    public void setSource(String source) {
        this.source = source;
    }
    @GuiIncluded(index = 10)
    public String getSource() {
        return source;
    }

    @GuiIncluded(description = "Obtains the local temprature data from JMA (Japan Meteorological Agency)")
    @GuiInits(action = @GuiInitAction(confirm = true))
    public void download() {
        try (var client = HttpClient.newBuilder()
                .build()) {
            var res = client.send(HttpRequest.newBuilder(URI.create(source))
                    .GET()
                    .build(), HttpResponse.BodyHandlers.ofString(Charset.forName("SJIS")));
            String data = res.body();
            String[] lines = data.split("[\\r\\n]+");
            tempraturesAll = Arrays.stream(lines)
                            .skip(1)
                            .map(this::create)
                                    .toList();
            System.out.printf("%d data %n", tempraturesAll.size());
            updateList();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    String filter = "";
    @GuiIncluded
    public void setFilter(String filter) {
        this.filter = filter;
        updateList();
    }
    @GuiIncluded(index = 20)
    public String getFilter() {
        return filter;
    }

    List<Temprature> tempraturesAll = List.of(new Temprature(
            new LocationInfo("テストデータ", "テストデータ"),
            new LocationInfo("TestData", "TestData"), 30));
    List<Temprature> tempratures = tempraturesAll;

    @GuiIncluded(index = 30)
    @GuiInits(table = @GuiInitTable(rowFitToContent = true, dynamicColumnAutoResize = true))
    public List<Temprature> getTempratures() {
        return tempratures;
    }

    private void updateList() {
        if (!filter.isEmpty()) {
            tempratures = tempraturesAll.stream()
                    .filter(t -> t.getStringForFilter().contains(filter))
                    .toList();
        } else {
            tempratures = new ArrayList<>(tempraturesAll);
        }
    }

    public Temprature create(String line) {
        String[] cols = line.split(",");
        String prefecture = col(cols, 1);
        String location = col(cols, 2);
        float max = colFloat(cols, 9);
        return new Temprature(prefecture, location, max);
    }

    private String col(String[] cols, int idx) {
        return idx < cols.length ? cols[idx] : "";
    }

    Pattern numPat = Pattern.compile("^[0-9.]+$");

    private float colFloat(String[] cols, int idx) {
        var n = col(cols, idx);
        if (numPat.matcher(n).matches()) {
            return Float.parseFloat(n);
        } else {
            return 0;
        }
    }

    @GuiIncluded
    public static class Temprature {
        LocationInfo location;
        LocationInfo locationEn;
        float maxCelsius;

        public Temprature(String prefecture, String location, float maxCelsius) {
            this.location = new LocationInfo(prefecture, location);
            this.locationEn = this.location.infoJpToEn();
            this.maxCelsius = maxCelsius;
        }

        public Temprature(LocationInfo location, LocationInfo locationEn, float maxCelsius) {
            this.location = location;
            this.locationEn = locationEn;
            this.maxCelsius = maxCelsius;
        }

        public String getStringForFilter() {
            return location.getStringForFilter() + " " + locationEn.getStringForFilter();
        }

        @GuiIncluded(index = 10)
        public LocationInfo getLocation() {
            return location;
        }
        @GuiIncluded(index = 20)
        public LocationInfo getLocationEn() {
            return locationEn;
        }

        @GuiIncluded(index = 30)
        @GuiInits(tableColumn = @GuiInitTableColumn(width = 80, sortOrder = SortOrder.DESCENDING),
                numberSpinner = @GuiInitNumberSpinner(format = "##.0"))
        public float getMaxCelsius() {
            return maxCelsius;
        }
    }

    @GuiIncluded
    public static class LocationInfo {
        String prefecture;
        String location;

        public LocationInfo(String prefecture, String location) {
            this.prefecture = (prefecture == null ? "" : prefecture.trim());
            this.location = (location == null ? "" : location.trim());
        }

        public String getStringForFilter() {
            return prefecture + " " + location;
        }

        @GuiIncluded
        @GuiInits(tableColumn = @GuiInitTableColumn(width = 350))
        public String getLocation() {
            return prefecture + "\n  " + location;
        }

        public LocationInfo infoJpToEn() {
            return new LocationInfo(jpToEn(prefecture), jpToEn(location));
        }
    }

    public static String jpToEn(String str) {
        List<String> words = new ArrayList<>();
        for (String[] trans : translations) {
            if (str.contains(trans[0])) {
                words.add(trans[1]);
            }
        }
        var m = rubiPat.matcher(str);
        if (m.find()) {
            String kana = m.group(1);
            StringBuilder kanaBuf = new StringBuilder();
            for (char c : kana.toCharArray()) {
                var name = Character.getName(c);
                var letterM = kanaNamePat.matcher(name);
                if (letterM.matches()) {
                    var letterStr = letterM.group(2);
                    if (letterStr.startsWith("SMALL ")) {
                        letterStr = letterStr.substring("SMALL ".length());
                    }
                    kanaBuf.append(letterStr);
                }
            }
            words.add(kanaBuf.toString());
        }
        return String.join(" ", words);
    }

    static Pattern rubiPat = Pattern.compile("（(.+)）"); //inside of JP parenthesis
    static Pattern kanaNamePat = Pattern.compile("(KATA|HIRA)KANA LETTER (.+)");

    static String[][] translations = {
            {"北海道宗谷地方", "Hokkaido Soya"}, {"北海道上川地方", "Hokkaido Kamikawa"}, {"北海道留萌地方", "Hokkaido Rumoi"},
            {"北海道石狩地方", "Hokkaido Ishikari"}, {"北海道空知地方", "Hokkaido Sorachi"}, {"北海道後志地方", "Hokkaido Goshi"},
            {"北海道網走・北見・紋別地方", "Hokkaido Abashiri Kitami Monbetsu"}, {"北海道根室地方", "Hokkaido Nemuro"},
            {"北海道釧路地方", "Hokkaido Kushiro"}, {"北海道十勝地方", "Hokkaido Tokachi"}, {"北海道胆振地方", "Hokkaido Iburi"},
            {"北海道日高地方", "Hokkaido Hidaka"}, {"北海道渡島地方", "Hokkaido Toshima"}, {"北海道檜山地方", "Hokkaido Hikiyama"},
            {"青森県", "Aomori"}, {"秋田県", "Akita"}, {"岩手県", "Iwate"}, {"宮城県", "Miyagi"}, {"山形県", "Yamagata"}, {"福島県", "Fukushima"},
            {"茨城県", "Ibaraki"}, {"栃木県", "Tochigi"}, {"群馬県", "Gunma"}, {"埼玉県", "Saitama"}, {"東京都", "Tokyo"}, {"千葉県", "Chiba"}, {"神奈川県", "Kanagawa"},
            {"長野県", "Nagano"}, {"山梨県", "Yamanashi"}, {"静岡県", "Shizuoka"}, {"愛知県", "Aichi"}, {"岐阜県", "Gifu"}, {"新潟県", "Niigata"},
            {"富山県", "Toyama"}, {"石川県", "Ishikawa"}, {"福井県", "Fukui"}, {"滋賀県", "Shiga"}, {"三重県", "Mie"},
            {"京都府", "Kyoto"}, {"大阪府", "Osaka"}, {"兵庫県", "Hyogo"}, {"奈良県", "Nara"}, {"和歌山県", "Wakayama"},
            {"岡山県", "Okayama"}, {"広島県", "Hiroshima"}, {"島根県", "Shimane"}, {"鳥取県", "Tottori"}, {"山口県", "Yamaguchi"},
            {"徳島県", "Tokushima"}, {"香川県", "Kagawa"}, {"愛媛県", "Ehime"}, {"高知県", "Kouchi"},
            {"福岡県", "Fukuoka"}, {"大分県", "Oita"}, {"長崎県", "Nagasaki"}, {"佐賀県", "Saga"}, {"熊本県", "Kumamoto"}, {"宮崎県", "Miyazaki"}, {"鹿児島県", "Kagoshima"}, {"沖縄県", "Okinawa"}};
}
