import DemoGrid from './Grid.vue'

export default {
  components: {
    DemoGrid
  },
  data: () => ({
    searchQuery: '',
    gridColumns: ['name', 'power'],
    gridData: [
      { name: '척 노리스', power: Infinity },
      { name: '브루스 리', power: 9000 },
      { name: '성룡', power: 7000 },
      { name: '이연걸', power: 8000 }
    ]
  })
}
