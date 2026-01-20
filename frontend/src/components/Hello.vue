<template>
  <div class="container">
    <div class="nav-container">
      <ul class="nav nav-tabs">
        <li class="active">
          <a data-toggle="tab" href="#i0">News Feed</a>
        </li>
        <li>
          <a data-toggle="tab" href="#i1">News Source</a>
        </li>
      </ul>
      <div class="tab-content">
        <div id="i0" class="tab-pane fade in active">
          <SourceSelection 
            v-bind:sourceOptions="sourceOptions"
            v-bind:dateRange="dateRange"
            v-bind:filters="filters"
            v-on:filtersChanged="applyFilters"
            v-on:refreshRequested="refreshSource">
          </SourceSelection>
          <Newslist v-bind:articles="filteredArticles">
          </Newslist>
        </div>
        <div id="i1" class="tab-pane">
          <FeedSourceForm></FeedSourceForm>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import SourceSelection from './SourceSelection'
import Newslist from './Newslist'
import FeedSourceForm from './FeedSourceForm'

export default {
  name: 'hello',
  data() {
    return {
      articles: [],
      filteredArticles: [],
      sourceOptions: [],
      dateRange: {min: '', max: ''},
      filters: {sourceName: '', from: '', to: ''}
    }
  },
  created() {
    this.loadArticles()
  },
  components: {
    Newslist,
    SourceSelection,
    FeedSourceForm
  },
  methods: {
    loadArticles() {
      const baseUrl = process.env.API_BASE_URL
      this.$http.get(`${baseUrl}/api/newsarticles`)
        .then(res => {
          const list = res.body.data
          this.articles = list
          this.filteredArticles = list
          this.refreshFiltersMeta(list)
        })
    },
    refreshFiltersMeta(list) {
      const sources = Array.from(new Set(list.map(a => a.sourceName).filter(Boolean)))
      this.sourceOptions = sources

      const dates = list.map(a => a.publishedAt).filter(Boolean).sort()
      this.dateRange = {
        min: dates.length ? dates[0] : '',
        max: dates.length ? dates[dates.length - 1] : ''
      }
    },
    applyFilters(newFilters) {
      this.filters = newFilters
      const { sourceName, from, to } = newFilters
      let result = this.articles

      if (sourceName) {
        result = result.filter(a => a.sourceName === sourceName)
      }
      if (from) {
        result = result.filter(a => a.publishedAt >= from)
      }
      if (to) {
        result = result.filter(a => a.publishedAt <= to)
      }

      this.filteredArticles = result
      this.refreshFiltersMeta(result)
    },
    refreshSource() {
      this.$toast.open({ message: `正在处理，请不要重复点击`, type: 'success' })
      const baseUrl = process.env.API_BASE_URL
      this.$http.get(`${baseUrl}/api/newsarticles/refresh`).then(res => {
        const list = res.body.data
        this.articles = this.articles.concat(list)
        this.applyFilters(this.filters)
        this.refreshFiltersMeta(this.filteredArticles)
        this.$toast.open({ message: `新增 ${list.length} 篇文章`, type: 'success' })
      })
    }
  },
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
.tab-pane{
  padding-top:20px;
}
</style>
