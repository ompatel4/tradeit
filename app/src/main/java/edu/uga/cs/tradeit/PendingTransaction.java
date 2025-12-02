package edu.uga.cs.tradeit;

public class PendingTransaction {

    private String id;
    private String buyerUid;
    private String sellerUid;
    private String catId;
    private String itemName;
    private long postedDate;
    private String price;

    public PendingTransaction() {
        // required for Firebase
    }

    public PendingTransaction(String id, String buyerUid, String sellerUid,
                              String catId, String itemName,
                              long postedDate, String price) {
        this.id = id;
        this.buyerUid = buyerUid;
        this.sellerUid = sellerUid;
        this.catId = catId;
        this.itemName = itemName;
        this.postedDate = postedDate;
        this.price = price;
    }

    public String getId() { return id; }
    public String getBuyerUid() { return buyerUid; }
    public String getSellerUid() { return sellerUid; }
    public String getCatId() { return catId; }
    public String getItemName() { return itemName; }
    public long getPostedDate() { return postedDate; }
    public String getPrice() { return price; }

    public void setId(String id) { this.id = id; }
    public void setBuyerUid(String buyerUid) { this.buyerUid = buyerUid; }
    public void setSellerUid(String sellerUid) { this.sellerUid = sellerUid; }
    public void setCatId(String catId) { this.catId = catId; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setPostedDate(long postedDate) { this.postedDate = postedDate; }
    public void setPrice(String price) { this.price = price; }
}
