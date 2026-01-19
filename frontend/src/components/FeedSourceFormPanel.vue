<template>
  <div class="feed-source-form-panel">
    <h4>Add Feed Source</h4>
    <form v-on:submit.prevent="submit">
      <div class="form-group">
        <label>Name</label>
        <input class="form-control" v-model="form.name" type="text" placeholder="Source name">
      </div>
      <div class="form-group">
        <label>URL</label>
        <input class="form-control" v-model="form.url" type="text" placeholder="https://example.com/rss">
      </div>
      <div class="form-group">
        <label>Source Type</label>
        <select class="form-control" v-model="form.sourceType">
          <option disabled value="">Please select</option>
          <option value="RSS">RSS</option>
          <option value="WEB">WEB</option>
        </select>
        <span>Selected: {{ form.sourceType }}</span>
      </div>
      <div class="form-group">
        <label>
          <input type="checkbox" v-model="form.enabled">
          Enabled
        </label>
      </div>
      <button class="btn btn-primary" type="submit">Create</button>
    </form>
  </div>
</template>
<script>
export default {
    name: 'feedsourceformpanel',
    data() {
        return {
            form: {
                name: '',
                url: '',
                sourceType: '',
                enabled: true
            }
        }
    },
    methods: {
        submit: function() {
            if (!this.form.name || this.form.name.trim() === '') {
                alert('name should not be null!')
                return
            }
            if (!this.form.url || this.form.url.trim() === '') {
                alert('url should not be null!')
                return
            }
            var params = new URLSearchParams()
            params.append('name', this.form.name)
            params.append('url', this.form.url)
            params.append('sourceType', this.form.sourceType)
            params.append('enabled', this.form.enabled)
            console.log(this.form)
            const baseUrl = process.env.API_BASE_URL
            this.$http.post(`${baseUrl}/feeds/new`, this.form, { emulateJSON: true }).then(()=>{
                this.$emit('created')
            })
        }
    }
}
</script>
