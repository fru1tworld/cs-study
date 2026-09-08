import { ref, computed } from 'vue'

export default {
  setup() {
    const flightType = ref('편도 항공권')
    const departureDate = ref(dateToString(new Date()))
    const returnDate = ref(departureDate.value)

    const isReturn = computed(() => flightType.value === '왕복 항공권')

    const canBook = computed(
      () =>
        !isReturn.value ||
        stringToDate(returnDate.value) > stringToDate(departureDate.value)
    )

    function book() {
      alert(
        isReturn.value
          ? `출발일이 ${departureDate.value}이고, 귀국일이 ${returnDate.value}인 왕복 항공권을 예약하셨습니다.`
          : `출발일이 ${departureDate.value}인 편도 항공권을 예약하셨습니다.`
      )
    }

    function stringToDate(str) {
      const [y, m, d] = str.split('-')
      return new Date(+y, m - 1, +d)
    }

    function dateToString(date) {
      return (
        date.getFullYear() +
        '-' +
        pad(date.getMonth() + 1) +
        '-' +
        pad(date.getDate())
      )
    }

    function pad(n, s = String(n)) {
      return s.length < 2 ? `0${s}` : s
    }

    return {
      flightType,
      departureDate,
      returnDate,
      isReturn,
      canBook,
      book
    }
  }
}
