<template>
  <div class="newslist">
    <div class="container">

      <vue-waterfall-easy
      ref="waterfall"
      v-bind:imgsArr="imgsArr"
      v-on:scrollReachBottom="getData"
      v-on:click="clickFn"
      v-on:imgError="imgErrorFn"
    >
      <div class="img-info" slot-scope="props">
        <p class="some-info">{{ props.value.title }}</p>
        <p class="some-info">{{ props.value.info }}</p>
      </div>
    </vue-waterfall-easy>
    </div>
  </div>
</template>

<script>
import vueWaterfallEasy from 'vue-waterfall-easy'
// https://github.com/lfyfly/vue-waterfall-easy/blob/master/README-CN.md
export default {
  name: 'newslist',
  props: ['source'],
  components: {
    vueWaterfallEasy
  },
  data() {
    return {
      imgsArr: []
    }
  },
  methods: {
    getData() {
      const baseUrl = process.env.API_BASE_URL
      this.$http.get(`${baseUrl}/api/newsarticles`).then(response => {
        const list = response.body && response.body.data ? response.body.data : response.body
        this.imgsArr = list.map(a => ({
          src: a.tumbnailURL,
          href: a.sourceURL,
          title: a.title,
          info: a.summary
        }))
        console.log(this.imgsArr)
      })
    },
    clickFn(event, {index, value}){
      if (event.target.tagName.toLowerCase() == 'img' && value.href) {
        window.open(value.href, '_blank')
      }
    },
    imgErrorFn(imgItem) {
      console.log('image load error', imgItem)
    }
  },
  created: function() {
    this.getData();
  },
}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
.newslist {
  height: 70vh;
}

.newslist .container {
  height: 100%;
}
</style>
