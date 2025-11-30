package edu.uga.cs.tradeit;

public class TradeItem {

    private String id;
    private String name;
    private String catId;
    private String posterUid;
    private long postedDate;
    private String price;   // "free" or "34.99"

    public TradeItem() {
        // empty constructor required by Firebase
    }

    public TradeItem(String id, String name, String catId,
                     String posterUid, long postedDate, String price) {
        this.id = id;
        this.name = name;
        this.catId = catId;
        this.posterUid = posterUid;
        this.postedDate = postedDate;
        this.price = price;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCatId() { return catId; }
    public String getPosterUid() { return posterUid; }
    public long getPostedDate() { return postedDate; }
    public String getPrice() { return price; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCatId(String catId) { this.catId = catId; }
    public void setPosterUid(String posterUid) { this.posterUid = posterUid; }
    public void setPostedDate(long postedDate) { this.postedDate = postedDate; }
    public void setPrice(String price) { this.price = price; }
}
