import { defineStore } from 'pinia'
import { ref } from 'vue'

export const usePostStore = defineStore('post', () => {
  const posts = ref([])
  const currentPost = ref(null)
  const hasMore = ref(true)
  const currentPage = ref(1)
  const pageSize = ref(15)

  function setPosts(newPosts) {
    posts.value = newPosts
  }

  function appendPosts(newPosts) {
    posts.value = [...posts.value, ...newPosts]
  }

  function setCurrentPost(post) {
    currentPost.value = post
  }

  function setHasMore(value) {
    hasMore.value = value
  }

  function setCurrentPage(page) {
    currentPage.value = page
  }

  function incrementPage() {
    currentPage.value++
  }

  function resetPage() {
    currentPage.value = 1
  }

  return {
    posts,
    currentPost,
    hasMore,
    currentPage,
    pageSize,
    setPosts,
    appendPosts,
    setCurrentPost,
    setHasMore,
    setCurrentPage,
    incrementPage,
    resetPage
  }
})
