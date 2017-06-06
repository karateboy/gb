<template>
    <div>
        <table class="table table-hover table-bordered table-condensed">
            <thead>
            <tr class='info'>
                <th>公立</th>
                <th>機構名稱</th>
                <th>負責人</th>
                <th>區域</th>
                <th>地址</th>
                <th>電話</th>
                <th>安養</th>
                <th>養護</th>
                <th>長照</th>
                <th>差兩管床數</th>
                <th>廢棄物</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="(careHouse, index) in myList" :class='{success: selectedIndex == index}'>
                <td>{{ displayIsPublic(careHouse.isPublic)}}</td>
                <td>{{ careHouse.name}}</td>
                <td>{{ careHouse.principal}}</td>
                <td>{{ careHouse.district}}</td>
                <td>{{ careHouse.addr}}</td>
                <td>{{ careHouse.phone}}</td>
                <td>{{ displayBed("安養", careHouse) }}</td>
                <td>{{ displayBed("養護", careHouse) }}</td>
                <td>{{ displayBed("長照", careHouse) }}</td>
                <td>{{ careHouse.beds}}</td>
                <td>{{ careHouse.waste}}</td>
            </tr>
            </tbody>
        </table>
    </div>
</template>
<style scoped>
    body {
    }


</style>
<script>
    import axios from 'axios'
    export default{
        props: {
            careHouseList: {
                type: Array,
                required: true
            }
        },
        data(){
            return {
                display: '',
                selectedIndex: -1,
                careHouse: {}
            }
        },
        computed: {
            myList(){
                return this.careHouseList;
            }
        },
        methods: {
            displayBed(careTypeName, careHouse){
                for (let careType of careHouse.careTypes) {
                    if (careType.name === careTypeName)
                        return careType.quantity
                }
                return 0
            },
            displayIsPublic(v){
                if (v) return "公立"
                else return "私立"
            },
            displayOrder(idx){
                this.selectedIndex = idx
                this.showOrder(this.myList[idx])
                this.display = 'detail';
            }
        },
        components: {}
    }
</script>
