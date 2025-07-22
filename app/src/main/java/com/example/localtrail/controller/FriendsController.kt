package com.example.localtrail.controller

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.localtrail.model.FriendRequest
import com.example.localtrail.model.Friend

object FriendsController {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * Sends a friend request to another user by adding the current user's UID to the target user's friendRequests array in Firestore.
     * If a request is already pending, the callback will return false and an informative exception.
     * @param targetUserId The UID of the user to whom the friend request is being sent.
     * @param onResult Callback with (success: Boolean, exception: Exception?) indicating the result of the operation.
     * @return None directly. The result is provided via the onResult callback.
     */
    fun sendFriendRequest(targetUserId: String, onResult: (Boolean, Exception?) -> Unit) {
        val currentUser = auth.currentUser ?: return onResult(false, Exception("User not logged in"))
        if (currentUser.uid == targetUserId) {
            return onResult(false, Exception("Cannot send friend request to yourself"))
        }
        val targetUserRef = db.collection("users").document(targetUserId)
        db.runTransaction { transaction ->
            val targetSnapshot = transaction.get(targetUserRef)
            val requests = try {
                targetSnapshot.get("friendRequests") as? List<String> ?: listOf()
            } catch (e: Exception) {
                listOf<String>()
            }
            if (requests.contains(currentUser.uid)) {
                throw Exception("Friend request already pending!")
            }
            if (!requests.contains(currentUser.uid)) {
                transaction.update(targetUserRef, "friendRequests", requests + currentUser.uid)
            }
        }.addOnSuccessListener { onResult(true, null) }
         .addOnFailureListener { e -> onResult(false, e) }
    }

    /**
     * Accepts a friend request from another user by removing the request and adding each user to the other's friends array in Firestore.
     * @param fromUserId The UID of the user whose friend request is being accepted.
     * @param onResult Callback with (success: Boolean, exception: Exception?) indicating the result of the operation.
     * @return None directly. The result is provided via the onResult callback.
     */
    fun acceptFriendRequest(fromUserId: String, onResult: (Boolean, Exception?) -> Unit) {
        val currentUser = auth.currentUser ?: return onResult(false, Exception("User not logged in"))
        val userRef = db.collection("users").document(currentUser.uid)
        val fromUserRef = db.collection("users").document(fromUserId)
        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val fromUserSnapshot = transaction.get(fromUserRef)
            val requests = try {
                userSnapshot.get("friendRequests") as? List<String> ?: listOf()
            } catch (e: Exception) {
                listOf<String>()
            }
            val friends = try {
                userSnapshot.get("friends") as? List<String> ?: listOf()
            } catch (e: Exception) {
                listOf<String>()
            }
            val fromUserFriends = try {
                fromUserSnapshot.get("friends") as? List<String> ?: listOf()
            } catch (e: Exception) {
                listOf<String>()
            }
            // Remove from friendRequests, add to friends
            if (requests.contains(fromUserId) && !friends.contains(fromUserId)) {
                transaction.update(userRef, "friendRequests", requests - fromUserId)
                transaction.update(userRef, "friends", friends + fromUserId)
                // Add current user to the other user's friends
                if (!fromUserFriends.contains(currentUser.uid)) {
                    transaction.update(fromUserRef, "friends", fromUserFriends + currentUser.uid)
                }
            }
        }.addOnSuccessListener { onResult(true, null) }
         .addOnFailureListener { e -> onResult(false, e) }
    }

    /**
     * Denies a friend request from another user by removing the request from the current user's friendRequests array in Firestore.
     * @param fromUserId The UID of the user whose friend request is being denied.
     * @param onResult Callback with (success: Boolean, exception: Exception?) indicating the result of the operation.
     * @return None directly. The result is provided via the onResult callback.
     */
    fun denyFriendRequest(fromUserId: String, onResult: (Boolean, Exception?) -> Unit) {
        val currentUser = auth.currentUser ?: return onResult(false, Exception("User not logged in"))
        val userRef = db.collection("users").document(currentUser.uid)
        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val requests = try {
                userSnapshot.get("friendRequests") as? List<String> ?: listOf()
            } catch (e: Exception) {
                listOf<String>()
            }
            if (requests.contains(fromUserId)) {
                transaction.update(userRef, "friendRequests", requests - fromUserId)
            }
        }.addOnSuccessListener { onResult(true, null) }
         .addOnFailureListener { e -> onResult(false, e) }
    }

    /**
     * Retrieves the list of friend UIDs for the current user from Firestore.
     * @param onResult Callback with (friends: List<String>?, exception: Exception?) containing the list of friend UIDs or an error.
     * @return None directly. The result is provided via the onResult callback.
     */
    fun getFriends(onResult: (List<Friend>?, Exception?) -> Unit) {
        val currentUser = auth.currentUser ?: return onResult(null, Exception("User not logged in"))
        val userRef = db.collection("users").document(currentUser.uid)

        userRef.get().addOnSuccessListener { document ->
            val friendIds = try {
                document.get("friends") as? List<String> ?: listOf()
            } catch (e: Exception) {
                listOf<String>()
            }

            if (friendIds.isEmpty()) {
                onResult(emptyList(), null)
                return@addOnSuccessListener
            }

            val friends = mutableListOf<Friend>()
            val tasks = friendIds.map { userId ->
                db.collection("users").document(userId).get().continueWith { task ->
                    val username = task.result?.getString("username") ?: "Unknown"
                    friends.add(Friend(userId, username))
                }
            }

            Tasks.whenAll(tasks).addOnSuccessListener {
                onResult(friends, null)
            }.addOnFailureListener { e ->
                onResult(null, e)
            }
        }.addOnFailureListener { e ->
            onResult(null, e)
        }
    }

    /**
     * Retrieves the list of friend request UIDs for the current user from Firestore.
     * @param onResult Callback with (requests: List<String>?, exception: Exception?) containing the list of friend request UIDs or an error.
     * @return None directly. The result is provided via the onResult callback.
     */
    fun getFriendRequests(onResult: (List<FriendRequest>?, Exception?) -> Unit) {
        val currentUser = auth.currentUser ?: return onResult(null, Exception("User not logged in"))
        val userRef = db.collection("users").document(currentUser.uid)

        userRef.get().addOnSuccessListener { document ->
            val requests = try {
                document.get("friendRequests") as? List<String> ?: listOf()
            } catch (e: Exception) {
                listOf<String>()
            }

            if (requests.isEmpty()) {
                onResult(emptyList(), null)
                return@addOnSuccessListener
            }

            val friendRequests = mutableListOf<FriendRequest>()
            val tasks = requests.map { userId ->
                db.collection("users").document(userId).get().continueWith { task ->
                    val username = task.result?.getString("username") ?: "Unknown"
                    friendRequests.add(FriendRequest(userId, username))
                }
            }

            Tasks.whenAll(tasks).addOnSuccessListener {
                onResult(friendRequests, null)
            }.addOnFailureListener { e ->
                onResult(null, e)
            }
        }.addOnFailureListener { e ->
            onResult(null, e)
        }
    }

    /**
     * Checks if the given user ID is a friend of the current user.
     * @param userId The UID of the user to check friendship status with.
     * @param onResult Callback with (isFriend: Boolean, exception: Exception?) indicating if the user is a friend or not, or an error.
     * @return None directly. The result is provided via the onResult callback.
     */
    fun isFriend(userId: String, onResult: (Boolean, Exception?) -> Unit) {
        val currentUser = auth.currentUser ?: return onResult(false, Exception("User not logged in"))
        val userRef = db.collection("users").document(currentUser.uid)
        userRef.get()
            .addOnSuccessListener { document ->
                val friends = try {
                    document.get("friends") as? List<String> ?: listOf()
                } catch (e: Exception) {
                    listOf<String>()
                }
                onResult(friends.contains(userId), null)
            }
            .addOnFailureListener { e -> onResult(false, e) }
    }

    /**
     * Retrieves the number of friends for the current user from Firestore.
     * @param onResult Callback with (count: Int?, exception: Exception?) containing the number of friends or an error.
     * @return None directly. The result is provided via the onResult callback.
     */
    fun getNumberOfFriends(onResult: (Int?, Exception?) -> Unit) {
        val currentUser = auth.currentUser ?: return onResult(null, Exception("User not logged in"))
        val userRef = db.collection("users").document(currentUser.uid)

        userRef.get().addOnSuccessListener { document ->
            val friends = try {
                document.get("friends") as? List<String> ?: listOf()
            } catch (e: Exception) {
                listOf<String>()
            }
            onResult(friends.size, null)
        }.addOnFailureListener { e ->
            onResult(null, e)
        }
    }

    /**
     * Removes a friend from both users' friends lists in Firestore.
     * @param friendUserId The UID of the friend to be removed.
     * @param onResult Callback with (success: Boolean, exception: Exception?) indicating the result of the operation.
     * @return None directly. The result is provided via the onResult callback.
     */
    fun removeFriend(friendUserId: String, onResult: (Boolean, Exception?) -> Unit) {
        val currentUser = auth.currentUser ?: return onResult(false, Exception("User not logged in"))
        val currentUserRef = db.collection("users").document(currentUser.uid)
        val friendUserRef = db.collection("users").document(friendUserId)
        
        db.runTransaction { transaction ->
            val currentUserSnapshot = transaction.get(currentUserRef)
            val friendUserSnapshot = transaction.get(friendUserRef)
            
            val currentUserFriends = try {
                currentUserSnapshot.get("friends") as? List<String> ?: listOf()
            } catch (e: Exception) {
                listOf<String>()
            }
            
            val friendUserFriends = try {
                friendUserSnapshot.get("friends") as? List<String> ?: listOf()
            } catch (e: Exception) {
                listOf<String>()
            }
            
            // Remove from both users' friends lists
            if (currentUserFriends.contains(friendUserId)) {
                transaction.update(currentUserRef, "friends", currentUserFriends - friendUserId)
            }
            
            if (friendUserFriends.contains(currentUser.uid)) {
                transaction.update(friendUserRef, "friends", friendUserFriends - currentUser.uid)
            }
        }.addOnSuccessListener { 
            onResult(true, null) 
        }.addOnFailureListener { e -> 
            onResult(false, e) 
        }
    }
}
