package data;

public class Child {
    private Parent parent;
    private String url;

    public Child(Parent parent, String url) {
        this.parent = parent;
        this.url = url;
    }

    public Parent getParent() {
        return parent;
    }

    public String getUrl() {
        return url;
    }
}
