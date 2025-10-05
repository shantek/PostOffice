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
    public String noPermission = "&a[Post Office] &4You don't have permission to do that.";
    public String denyAction = "&a[Post Office] &4You can't do that here!";
    public String notRegistered = "&a[Post Office] &4This isn't a registered post office box.";
    public String postBoxRemoved = "&a[Post Office] &aPost box removed successfully.";
    public String successfulRegistration = "&a[Post Office] &aPost box registered successfully.";
    public String alreadyRegistered = "&a[Post Office] &4This post box is already registered.";
    public String postboxCreated = "&a[Post Office] &4 Box successfully created for %username%";
    public String removeFromConfig = "&a[Post Office] &aPost box successfully removed from the config.";
    public String lookAtPostBox = "&a[Post Office] &4You must be looking at a barrel or a sign attached to a barrel.";
    public String signOnBarrel = "&a[Post Office] &4The sign must be attached to a barrel.";
    public String alreadyClaimed = "&a[Post Office] &4This post box has already been claimed.";
    public String invalidPostbox = "&a[Post Office] &4This isn't a valid post box.";
    public String successfullyClaimed = "&a[Post Office] &aYou have successfully registered this post box.";
    public String modifySign = "&a[Post Office] &4You cannot modify a post box sign.";
    public String unclaimedPostbox = "&a[Post Office] &4This post box is unclaimed.";
    public String userBanned = "&a[Post Office] &4You aren't able to interact with this post box.";
    public String postBoxOwner = "&a[Post Office] &aThis post box is owned by %owner%";
    public String claimedFor = "&a[Post Office] &aThis post box has been claimed for %owner%";
    public String alreadyHasPostBox = "&a[Post Office] &4%player% already has a post box at: %location%";
    public String notPlayedBefore = "&a[Post Office] &4The player %player% has not played on this server.";
    public String claimedForOtherPlayer = "&a[Post Office] &aA post box has been created for you.";
    public String pluginUpToDate = "Your plugin is up-to-date.";
    public String blacklistedItem = "&a[Post Office] &4This item is blacklisted item.";

    public UpdateChecker updateChecker;
    public PluginConfig pluginConfig;

}
