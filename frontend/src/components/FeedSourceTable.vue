<template>
  <div class="feed-source-table">
    <table class="table table-striped">
      <thead>
        <tr>
          <th>Name</th>
          <th>URL</th>
          <th>Source Type</th>
          <th>Enabled</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="source in sources" v-bind:key="source.id">
          <td>{{ source.name }}</td>
          <td>{{ source.url }}</td>
          <td>{{ source.sourceType }}</td>
          <td>
            <input type="checkbox"
                   v-model="source.enabled"
                   v-on:change="toggle(source)">
          </td>
        </tr>
        <tr v-if="!sources || sources.length === 0">
          <td colspan="4">No sources</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  name: 'feedsourcetable',
  props: ['sources'],
  methods: {
    toggle: function(source) {
      this.$emit('toggle', source)
    }
  }
}
</script>

<style scoped>
.feed-source-table {
  margin-top: 20px;
}
</style>
