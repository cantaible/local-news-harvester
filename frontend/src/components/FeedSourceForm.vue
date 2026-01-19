<template>
  <div class="feed-source-form">
    <FeedSourceFormPanel v-on:created="loadSources"></FeedSourceFormPanel>
    <FeedSourceTable v-bind:sources="sources" v-on:toggle="handleToggle"></FeedSourceTable>
  </div>
</template>

<script>
import FeedSourceFormPanel from './FeedSourceFormPanel'
import FeedSourceTable from './FeedSourceTable'

export default {
  name: 'feedsourceform',
  components: {
    FeedSourceFormPanel,
    FeedSourceTable
  },
  data() {
    return {
      sources: []
    }
  },
  created: function() {
    this.loadSources()
  },
  methods: {
    loadSources: function() {
      const baseUrl = process.env.API_BASE_URL
      this.$http.get(`${baseUrl}/api/feeditems`)
        .then(response => {
          if (response.body && response.body.data) {
            this.sources = response.body.data
          } else {
            this.sources = response.body
          }
        })
    },
    handleToggle: function(source) {
      // TODO: add update API call when backend supports it
      alert("TODO: add update API call when backend supports it")
    }
  }
}
</script>
