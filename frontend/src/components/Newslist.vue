<template>
  <div class="newslist">
    <div class="container">

      <vue-waterfall-easy
      ref="waterfall"
      v-bind:imgsArr="imgsArr"
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
  props: ['articles'],
  components: {
    vueWaterfallEasy
  },
  data() {
    return {
      imgsArr: []
    }
  },
  watch: {
    articles: function(val) {
      this.imgsArr = (val || []).map(a => ({
        src: a.tumbnailURL,
        href: a.sourceURL,
        title: a.title,
        info: a.summary
      }))
    }
  },
  methods: {
    clickFn(event, {index, value}){
      if (event.target.tagName.toLowerCase() == 'img' && value.href) {
        window.open(value.href, '_blank')
      }
    },
    imgErrorFn(imgItem) {
      console.log('image load error', imgItem)
    }
  }
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
