<template>
  <div class="container source-selection">
      <div class="jumbotron">
        <h2>
          <span class="glyphicon glyphicon-list-alt"></span>&nbsp;News List
        </h2>
        <h4>Select News Source</h4>
        <select class="form-control" v-on:change="sourceChanged">
          <option value="">Please select news source...</option>
          <option v-for="source in sources" v-bind:value="source.id">{{ source.name }}</option>
        </select>
      </div>
  </div>
</template>

<script>
export default {
  name: 'sourceselection',
  data() {
    return {
      sources: [],
      source: ''
    }
  },
  methods: {
    sourceChanged: function(evt) {
      for (var i = 0; i < this.sources.length; i++) {
        if (String(this.sources[i].id) === evt.target.value) {
          this.source = this.sources[i];
        }
      }
      this.$emit('sourceChanged', evt.target.value);
    }
  },
  created: function() {
    const baseUrl = process.env.API_BASE_URL
    this.$http.get(`${baseUrl}/api/feeditems`)
      .then(response => {
        if (response.body && response.body.data) {
          this.sources = response.body.data;
        } else {
          this.sources = response.body;
        }
      });
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>

</style>
