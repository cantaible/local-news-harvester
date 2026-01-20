<template>
  <div class="container source-selection">
      <div class="jumbotron">
        <h2>
          <span class="glyphicon glyphicon-list-alt"></span>&nbsp;News Card
        </h2>
        <h4>Select Filter</h4>
        <div class="filter-panel">
        <div class="form-group">
          <label>Source</label>
          <select class="form-control" v-model="localFilters.sourceName" v-on:change="emitFilters">
            <option value="">All</option>
            <option v-for="s in sourceOptions" v-bind:key="s" v-bind:value="s">{{ s }}</option>
          </select>
        </div>

        <div class="form-group">
          <label>From</label>
          <input class="form-control" type="date" v-model="localFilters.from" v-on:change="emitFilters">
        </div>

        <div class="form-group">
          <label>To</label>
          <input class="form-control" type="date" v-model="localFilters.to" v-on:change="emitFilters">
        </div>
      </div>
      <button class="btn btn-primary" v-on:click="requestRefresh">Refresh</button>
      </div>
  </div>
</template>

<script>
export default {
  name: 'sourceselection',
  props: ['sourceOptions', 'dateRange', 'filters'],
  data() {
    return {
      localFilters: { sourceName: '', from: '', to: '' }
    }
  },
  watch: {
    filters: {
      immediate: true,
      handler(val) {
        this.localFilters = Object.assign({}, val)
      }
    }
  },
  methods: {
    emitFilters() {
      this.$emit('filtersChanged', Object.assign({}, this.localFilters))
      // 父组件 v-bind 把 filters/sourceOptions/dateRange 给子组件
      // 子组件 watch(filters) 把父的 filters 拷贝进 localFilters，表单显示对应值
      // 用户改表单 → 改的是 localFilters（v-model）
      // 子组件 emit('filtersChanged', localFilters拷贝)
      // 父组件 applyFilters 接收新条件 → 更新 filters 和 filteredArticles
      // 父组件再次 v-bind 新状态 → 子组件再次同步显示
    },
    requestRefresh() {
      this.$emit('refreshRequested')

    }
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>

</style>
