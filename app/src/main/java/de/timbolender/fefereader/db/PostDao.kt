package de.timbolender.fefereader.db

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PostDao {
    companion object {
        const val POSTS_QUERY: String = "SELECT * FROM post ORDER BY timestampId DESC"
        const val UNREAD_POSTS_QUERY: String = "SELECT * FROM post WHERE isRead = 0 ORDER BY timestampId DESC"
        const val BOOKMARK_POSTS_QUERY: String = "SELECT * FROM post WHERE isBookmarked = 1 ORDER BY timestampId DESC"

        const val SINGLE_POST_QUERY: String = "SELECT * FROM post WHERE id = :postId"
    }

    // Full data

    @Query(POSTS_QUERY)
    fun loadAllPostsPaged(): DataSource.Factory<Int, Post>

    @Query(UNREAD_POSTS_QUERY)
    fun loadUnreadPostsPaged(): DataSource.Factory<Int, Post>

    @Query(BOOKMARK_POSTS_QUERY)
    fun loadBookmarkedPostsPaged(): DataSource.Factory<Int, Post>

    // Single data

    @Query(SINGLE_POST_QUERY)
    fun loadPostSync(postId: Long): Post

    @Query(SINGLE_POST_QUERY)
    fun loadPost(postId: Long): LiveData<Post>

    // Insert/Update

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPost(post: Post): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPosts(vararg posts: Post): Array<Long>

    // Count

    @Query("SELECT COUNT(*) FROM post WHERE isRead = 0")
    fun loadUnreadCount(): Long

    @Query("SELECT COUNT(*) FROM post WHERE isBookmarked = 1")
    fun loadBookmarkedCount(): Long
}
