<template>
    <div>
        <div v-if="buildCaseList.length != 0">
            <table class="table table-hover table-bordered table-condensed">
                <thead>
                <tr class='info'>
                    <th></th>
                    <th>預警</th>
                    <th>縣市</th>
                    <th>起造人案名</th>
                    <th>建築師</th>                    
                    <th>樓板面積</th>
                    <th>地址</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="(buildCase, index) in buildCaseList" :class='{success: selectedIndex == index}'>
                    <td>
                        <button class="btn btn-primary" @click="editBuildCase(index)"><i class="fa fa-pen"></i>&nbsp;編輯</button>                    
                    </td>
                    <td>{{ alertInfo(buildCase)}}</td>
                    <td>{{ buildCase.county}}</td>
                    <td>{{ buildCase.name}}</td>
                    <td>{{ buildCase.architect}}</td>
                    <td>{{ buildCase.area}}</td>
                    <td>{{ buildCase.addr}}</td>
                </tr>
                </tbody>
            </table>
            <pagination for="cardList" :records="total" :per-page="5"
                        count-text="第{from}到第{to}筆/共{count}筆|{count} 筆|1筆"></pagination>
        </div>
        <div v-else class="alert alert-info" role="alert">沒有符合的機構</div>
        <build-case-detail v-if="display === 'detail'" :buildCase="buildCaseList[selectedIndex]"></build-case-detail>
    </div>
</template>
<style scoped>
</style>
<script>
    import axios from 'axios'
    import {Pagination, PaginationEvent} from 'vue-pagination-2'
    import buildCaseDetail from './buildCaseDetail.vue'

    export default {
        props: {
            url: {
                type: String,
                required: true
            },
            param: {
                type: Object
            }
        },
        data() {
            return {
                buildCaseList: [],
                skip: 0,
                limit: 5,
                total: 0,
                display: "",
                selectedIndex: -1
            }
        },
        mounted: function () {
            this.fetchbuildCase(this.skip, this.limit)
            PaginationEvent.$on('vue-pagination::cardList', this.handlePageChange)
        },
        watch: {
            url: function (newUrl) {
                this.fetchbuildCase(this.skip, this.limit)
            },
            param: function (newParam) {
                this.fetchbuildCase(this.skip, this.limit)
            }
        },

        methods: {
            processResp(resp) {
                const ret = resp.data
                this.buildCaseList.splice(0, this.buildCaseList.length)

                for (let buildCase of ret) {
                    this.buildCaseList.push(buildCase)
                }
            },
            fetchbuildCase(skip, limit) {
                let request_url = `${this.url}/${skip}/${limit}`

                if (this.param) {
                    axios.post(request_url, this.param).then(this.processResp).catch((err) => {
                        alert(err)
                    })
                } else {
                    axios.get(request_url).then(this.processResp).catch((err) => {
                        alert(err)
                    })
                }
                this.fetchbuildCaseCount()
            },
            fetchbuildCaseCount() {
                let request_url = `${this.url}/count`
                if (this.param) {
                    axios.post(request_url, this.param).then(resp => {
                        this.total = resp.data
                    }).catch((err) => {
                        alert(err)
                    })
                } else {
                    axios.get(request_url).then(resp => {
                        this.total = resp.data
                    }).catch((err) => {
                        alert(err)
                    })
                }
            },
            handlePageChange(page) {
                this.skip = (page - 1) * this.limit
                this.fetchbuildCase(this.skip, this.limit)
            },
            editbuildCase(idx) {
                this.selectedIndex = idx
                this.display = 'detail';
            }
        },
        components: {
            Pagination,
            buildCaseDetail
        }
    }
</script>
