<template>
    <div class="ibox flot-e-margins">
        <div class="ibox-content">
            <ul class="nav nav-tabs">
                <router-link to='/BuildCase/Query' tag='li' role="presentation" active-class='active'><a>查詢起造人</a>
                </router-link>
            </ul>
            <router-view></router-view>
        </div>
    </div>
</template>
<style scoped>
</style>
<script>
    export default{
        data(){
            return {
            }
        },
        components: {}
    }
</script>
