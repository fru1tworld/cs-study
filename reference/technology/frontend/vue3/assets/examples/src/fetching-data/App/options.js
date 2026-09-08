const API_URL = `https://api.github.com/repos/vuejs/core/commits?per_page=3&sha=`

export default {
  data: () => ({
    branches: ['main', 'minor'],
    currentBranch: 'main',
    commits: []
  }),

  created() {
    // 초기화 시 데이터 가져오기
    this.fetchData()
  },

  watch: {
    // currentBranch가 변경될 때마다 다시 데이터 가져오기
    currentBranch: 'fetchData'
  },

  methods: {
    async fetchData() {
      const url = `${API_URL}${this.currentBranch}`
      this.commits = await (await fetch(url)).json()
    },
    truncate(v) {
      const newline = v.indexOf('\n')
      return newline > 0 ? v.slice(0, newline) : v
    },
    formatDate(v) {
      return v.replace(/T|Z/g, ' ')
    }
  }
}
