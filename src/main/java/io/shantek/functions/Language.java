package io.shantek.functions;

import io.shantek.PostOffice;

public class Language {

    public PostOffice postOffice;
    public Language(PostOffice postOffice) {
        this.postOffice = postOffice;
    }

    public String lookAtPostBox = "&a[Post Office] &4You must be looking at a barrel or a sign attached to a barrel.";
    public String notRegistered = "&a[Post Office] &4This isn't a registered post office box.";
    public String alreadyRegistered = "&a[Post Office] &4This post box is already registered.";
    public String postBoxRemoved = "&a[Post Office] &aPost box removed successfully.";
    public String signOnBarrel = "&a[Post Office] &4The sign must be attached to a barrel.";
    public String successfulRegistration = "&a[Post Office] &aPost box registered successfully.";
    public String alreadyClaimed = "&a[Post Office] &4This post box has already been claimed.";
    public String successfullyClaimed = "&a[Post Office] &aYou have successfully registered this post box.";
    public String modifySign = "&a[Post Office] &4You cannot modify a post box sign.";
    public String removeFromConfig = "&a[Post Office] &aPost box successfully removed from the config.";
    public String unclaimedPostbox = "&a[Post Office] &4This post box is unclaimed.";
    public String userBanned = "&a[Post Office] &4You aren't able to interact with this post box.";
    public String invalidPostbox = "&a[Post Office] &4This isn't a valid post box.";


    public String sentMessage = "&a[Post Office] &aMail sent to %receiver%.";
    public String receivedMessage = "&a[Post Office] &eYou received mail from %sender%!";
    public String gotMailMessage = "&a[Post Office] &fYou got mail!";
    public String noPermission = "&a[Post Office] &4You don't have permission to do that.";
    public String removeItemError = "&a[Post Office] &4You don't have permission to remove items.";
    public String hotBarError = "&a[Post Office] &4No hot bar usage while in a Post Box!";
    public String breakError = "&a[Post Office] &4You can't break a Post Box.";
    public String createError = "&a[Post Office] &4You can't create a Post Box.";
    public String postboxCreated = "&a[Post Office] &4 Box successfully created for %username%";
    public String pluginUpToDate = "Your plugin is up-to-date.";
    public String dropItemError = "&a[Post Office] &4 You can't drop items while in a postbox.";

    public UpdateChecker updateChecker;
    public PluginConfig pluginConfig;

}
