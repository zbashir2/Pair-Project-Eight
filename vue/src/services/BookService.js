import axios from 'axios';
const apiKey = 'AIzaSyB4tzPEX2DuuR9N4255UTT0VNMonhWfy2A'
export default {

    getBookFromAPI(title) {
        delete axios.defaults.headers.common["Authorization"];
        return axios.get(`https://www.googleapis.com/books/v1/volumes?q=${title}&key=${apiKey}&max_results=1`)
    },

    addBook(book) {
        return axios.post('/books', book)
    },
    getbooks() {
        return axios.get('/books')
    },
    updateSearch(){
        return axios.post('/search')
    },
    getLastSearchDate() {
        return axios.get('/search')
    },
    getGenres() {
        return axios.get('/genres')
    }

}