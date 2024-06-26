package io.shantek.functions;

import io.shantek.PostOffice;

public class Language {

    public PostOffice postOffice;
    public Language(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    public String sentMessage = "&a[Post Office] &aMail sent to %receiver%.";
    public String receivedMessage = "&a[Post Office] &eYou received mail from %sender%!";
    public String gotMailMessage = "&a[Post Office] &fYou got mail!";
    public String cantStackItems = "&a[Post Office] &4You don't have permission to do that.";
    public String removeItemError = "&a[Post Office] &4You don't have permission to remove items.";
    public String offHandError = "&a[Post Office] &4No offhand usage while in a Post Box!";
    public String hotBarError = "&a[Post Office] &4No hot bar usage while in a Post Box!";
    public String breakError = "&a[Post Office] &4You can't break a Post Box.";
    public String createError = "&a[Post Office] &4You can't create a Post Box.";
    public String postboxCreated = "&a[Post Office] &4 Box successfully created for %username%";
    public String pluginUpToDate = "Your plugin is up-to-date.";
    public String dropItemError = "&a[Post Office] &4 You can't drop items while in a postbox.";
    public UpdateChecker updateChecker;
    public PluginConfig pluginConfig;

}
