<template>
  <div class="newslist">
    <div class="container">
      <ul class="media-list">
        <li class="media" v-for="article in articles">
          <div class="media-left">
            <a v-bind:href="article.url" target="_blank">
              <img class="media-object" v-bind:src="article.urlToImage">
            </a>
          </div>
          <div class="media-body">
            <h4 class="media-heading">
              <a v-bind:href="article.url" target="_blank">{{ article.title }}</a>
            </h4>
            <h5>
              <i>By {{ article.author }}</i>
            </h5>
            <p>{{ article.description }}</p>
          </div>
          <div class="media-right">
            <span class="glyphicon glyphicon-heart" v-on:click="addFavourite(article, $event)"></span>
          </div>
        </li>
      </ul>
    </div>
  </div>
</template>

<script>
import { db } from '../db'

export default {
  name: 'newslist',
  props: ['source'],
  data() {
    return {
      articles: []
    }
  },
  methods: {
    updateSource: function(source) {
      if (source) {
        this.$http.get('https://newsapi.org/v2/top-headlines?sources=' + source + '&apiKey=490cb422b79149ae85bdf2b85d62a848')
          .then(response => {
            this.articles = response.body.articles;
          });
      }
    },
    addFavourite: function(article, evt) {
      db.ref('articles').push(article)
      evt.target.style.visibility = 'hidden';//hide heart icon
    },
  },
  created: function() {
    this.updateSource(this.source);
  },
  watch: {
    source: function(val) {
      this.updateSource(val);
    }
  }
}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>

</style>
