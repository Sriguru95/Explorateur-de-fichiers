import java.util.List;
import java.util.Map;

public class ExplorerConfig {
    private List<String> favoris;
    private Map<String, String> viewers; 

    public List<String> getFavoris() {
        return favoris;
    }

    public void setFavoris(List<String> favoris) {
        this.favoris = favoris;
    }

    public Map<String, String> getViewers() {
        return viewers;
    }

    public void setViewers(Map<String, String> viewers) {
        this.viewers = viewers;
    }
}
